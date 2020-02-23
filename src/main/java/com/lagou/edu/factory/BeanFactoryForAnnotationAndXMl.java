package com.lagou.edu.factory;

import com.lagou.edu.annotation.AutoWrite;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.pojo.BeanDefinition;
import com.lagou.edu.pojo.PropertyRef;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class BeanFactoryForAnnotationAndXMl {

    private static Map<String,Object> map = new HashMap<>();  // 存储对象
    private static Map<String,Object> mapTwo = new HashMap<>();
    private static Map<String,Object> mapThree = new HashMap<>();

    private static Map<String,BeanDefinition> beanDefinitions = new HashMap<>();
    private final   static ProxyFactory proxyFactory =new ProxyFactory();

    /**
     *  解析xml 找到扫描包，将加了@Service的注解跟 xml配置的注解解析成beanDefinition
     *  根据beanDefinitions循环生成bean 并添加到map中
     *
     * */

    static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactoryForAnnotationAndXMl.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            //将xml与添加了注解的类转换成BeanDifinition
            doXMLHandler(document);
            getBean();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }

    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

    private static void doXMLHandler(Document document) throws ClassNotFoundException,IllegalAccessException,InstantiationException{
        Element rootElement = document.getRootElement();
        List<Element> beanList = rootElement.selectNodes("//bean");
        for (int i = 0; i < beanList.size(); i++) {
            Element element =  beanList.get(i);
            // 处理每个bean元素，获取到该元素的id 和 class 属性
            BeanDefinition beanDefinition = new BeanDefinition();
            BeanDefinition dbBeandefinition = beanDefinitions.get(element.attributeValue("id"));
            if(dbBeandefinition!=null){
                System.out.println("id重复");
                return;
            }

            beanDefinition.setId(element.attributeValue("id"));
            beanDefinition.setClazz(element.attributeValue("class"));
            List<Element> property = element.elements("property");
            for (Element e : property) {
                PropertyRef propertyRef = new PropertyRef();
                propertyRef.setName(e.attributeValue("name"));
                propertyRef.setRef(e.attributeValue("ref"));
                beanDefinition.getRefs().add(propertyRef);
            }
            beanDefinitions.put(beanDefinition.getId(),beanDefinition);

        }
        //解析xml扫描注解
        List<Element> packageElement = rootElement.selectNodes("//component-scan");
        if(packageElement.size()>0) {
            String packagePathStr = packageElement.get(0).attributeValue("//base-package");
            String []packages = packagePathStr.split(",");
            for (String packagePath : packages) {
                doScan(packagePath);
            }
        }

    }

    /**
     * 扫描包下文件
     * **/
    private static void doScan(String packageStr){
        Set<Class<?>> classes = new LinkedHashSet<>();
        String pkgDirName = packageStr.replace('.', '/');
        try {
            Enumeration<URL> urls = BeanFactoryForAnnotationAndXMl.class.getClassLoader().getResources(pkgDirName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");// 获取包的物理路径
                System.out.println(filePath);
                findClassesByFile(packageStr, filePath, classes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描包路径下的所有class文件
     *
     * @param pkgName 包名
     * @param pkgPath 包对应的绝对地址
     * @param classes 保存包路径下class的集合
     */
    private static void findClassesByFile(String pkgName, String pkgPath, Set<Class<?>> classes) {
        File dir = new File(pkgPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }


        // 过滤获取目录，or class文件
        File[] dirfiles = dir.listFiles(pathname -> pathname.isDirectory() || pathname.getName().endsWith("class"));


        if (dirfiles == null || dirfiles.length == 0) {
            return;
        }


        String className;
        Class clz;
        for (File f : dirfiles) {
            if (f.isDirectory()) {
                findClassesByFile(pkgName + "." + f.getName(),
                        pkgPath + "/" + f.getName(),
                        classes);
                continue;
            }


            // 获取类名，干掉 ".class" 后缀
            className = f.getName();
            className = className.substring(0, className.length() - 6);
            // 判断处理@Service类注解与@Autowirte注解
             loadClass(className,pkgName + "." + className);

        }
    }

    private static void loadClass(String className,String fullClzName) {
        try {
            Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(fullClzName);
            //将有@Service的类转化成beanDefinition
            if(aClass.isAnnotationPresent(com.lagou.edu.annotation.Service.class)){
                Service annotation = aClass.getAnnotation(com.lagou.edu.annotation.Service.class);
                BeanDefinition beanDefinition = new BeanDefinition();
                if(!"".equals(annotation.value().toString())|| annotation.value()!=null){
                    beanDefinition.setId(annotation.value().toString());
                }else {
                    beanDefinition.setId(className.toLowerCase());
                }
               beanDefinition.setClazz(fullClzName);
                if(aClass.isAnnotationPresent(com.lagou.edu.annotation.Transaction.class)){
                    beanDefinition.setTransaction(true);
                }else {
                    beanDefinition.setTransaction(false);
                }
                //处理@AutoWrite
                Field[] declaredFields = aClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(com.lagou.edu.annotation.AutoWrite.class))
                    {
                        String typeName = declaredField.getGenericType().getTypeName();
                        String[] split = typeName.split("\\.");
                        String name = split[split.length-1];
                        PropertyRef propertyRef = new PropertyRef();
                        AutoWrite propertyAnnotation = aClass.getAnnotation(com.lagou.edu.annotation.AutoWrite.class);

                        if(!"".equals(propertyAnnotation.value().toString())|| propertyAnnotation.value()!=null){
                            propertyRef.setRef(propertyAnnotation.value().toString().toLowerCase());
                            propertyRef.setName(propertyAnnotation.value().toString());
                        }else {
                            propertyRef.setRef(name.toLowerCase());
                            propertyRef.setName(name);
                        }


                        beanDefinition.getRefs().add(propertyRef);
                    }

                }
                beanDefinitions.put(beanDefinition.getId(),beanDefinition);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //将BeanDefinition循环实例化，并加入bean容器中
    private static void doGetBean(BeanDefinition beanDefinition) throws ClassNotFoundException,InstantiationException,IllegalAccessException,InvocationTargetException{

            Object mapO = map.get(beanDefinition.getId());
            if(mapO != null){
                return;
            }

            Class<?> aClass = Class.forName(beanDefinition.getClazz());
            Object o = aClass.newInstance();  // 实例化之后的对象

            // 存储到map中待用
            mapThree.put(beanDefinition.getId(),o);
            List<PropertyRef> refs = beanDefinition.getRefs();
            if(refs.size()>0){
                for (PropertyRef propertyRef : refs) {
                    if(map.get(propertyRef.getRef())!=null){
                        setPropertyBeanByMap(beanDefinition.getId(),propertyRef,"one");
                    }else if(mapThree.get(propertyRef.getRef())!=null){
                        setPropertyBeanByMap(beanDefinition.getId(),propertyRef,"three");
                    }else {
                        doGetBean(beanDefinitions.get(propertyRef.getRef()));
                    }

                }
            }

            map.put(beanDefinition.getId(),mapTwo.get(beanDefinition.getId()));
            mapTwo.remove(beanDefinition.getId());
            if(beanDefinition.getTransaction()) {
                doTransactionComponentProxy(beanDefinition.getId());
            }
    }

    private static void setPropertyBeanByMap(String id,PropertyRef propertyRef,String type) throws InvocationTargetException,IllegalAccessException{
        Object parentObject = mapThree.get(id);
        // 遍历父对象中的所有方法，找到"set" + name
        Method[] methods = parentObject.getClass().getMethods();
        for (int j = 0; j < methods.length; j++) {
            Method method = methods[j];
            if(method.getName().equalsIgnoreCase("set" + propertyRef.getName())) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                if("one".equals(type)) {
                    method.invoke(parentObject, map.get(propertyRef.getRef()));
                }else {
                    method.invoke(parentObject,mapThree.get(propertyRef.getRef()));

                }
            }
        }
        if("three".equals(type)){
            mapTwo.put(propertyRef.getRef(),mapThree.get(propertyRef.getRef()));
            mapThree.remove(propertyRef.getRef());
        }
        mapTwo.put(id,parentObject);
        mapThree.remove(id);
    }

    private static void getBean() throws ClassNotFoundException,InstantiationException,IllegalAccessException,InvocationTargetException{
        for(BeanDefinition beanDefinition:beanDefinitions.values()){
            doGetBean(beanDefinition);
        }
    }
    private static void doTransactionComponentProxy(String id){
        Object o = map.get(id);
        Object newO;
        if(o.getClass().isInterface()){
            newO = proxyFactory.getJdkProxy(o);
        }else {
            newO = proxyFactory.getCglibProxy(o);
        }
        map.put(id,newO);
    }


}
