package kim.hsl.annotation_compiler;

import com.google.auto.service.AutoService;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import kim.hsl.annotation.BindView;

/**
 * 生成代码的注解处理器
 */
@AutoService(Processor.class)
public class Compiler extends AbstractProcessor {

    /**
     * 生成 Java 代码对象
     */
    private Filer mFiler;

    /**
     * 日志打印
     */
    private Messager mMessager;

    /**
     * 初始化注解处理器相关工作
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.mFiler = processingEnv.getFiler();
        this.mMessager = processingEnv.getMessager();
    }

    /**
     * 声明 注解处理器 要处理的注解类型
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new HashSet<String>();
        // 将 BindView 全类名 kim.hsl.annotation.BinndView 放到 Set 集合中
        supportedAnnotationTypes.add(BindView.class.getCanonicalName());
        return supportedAnnotationTypes;
    }

    /**
     * 声明支持的 JDK 版本
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        // 通过 ProcessingEnvironment 类获取最新的 Java 版本并返回
        return processingEnv.getSourceVersion();
    }

    /**
     * 搜索 Android 代码中的 BindView 注解
     * 并生成相关代码
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // 搜索 BindView , 将 BindView 注解放在什么元素上 , 得到的就是相应类型的元素
        // 根据 注解类型 获取 被该注解类型 标注的元素 , 元素可能是类 , 方法 , 字段 ;
        // 通过 getElementsAnnotatedWith 方法可以搜索到整个 Module 中所有使用了 BindView 注解的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);

        // @BindView 注解标注的都是 Activity 中的成员字段,
        // 上述 elements 中的元素都是 VariableElement 类型的节点
        HashMap<String, ArrayList<VariableElement>> elementMap = new HashMap<>();

        // 遍历 elements 注解节点 , 为节点分组
        for (Element element : elements){
            // 将注解节点类型强转为 VariableElement 类型
            VariableElement ve = (VariableElement) element;

            // 获取该注解节点对应的成员变量类名
            // 先获取该注解节点的上一级节点 , 注解节点是 VariableElement , 成员字段节点
            // 上一级节点是就是 Activity 类节点对应的 类节点 TypeElement
            TypeElement te = (TypeElement) ve.getEnclosingElement();

            // 获取 Activity 的全类名
            String activityFullName = te.getQualifiedName().toString();

            mMessager.printMessage(Diagnostic.Kind.NOTE, "TypeElement : " + activityFullName + " , VariableElement : " + ve.getSimpleName());

            // 获取 elementMap 集合中的 Activity 的全类名对应的 VariableElement 节点集合
            // 如果是第一次获取 , 为空 ,
            // 如果之前已经获取了该 Activity 的全类名对应的 VariableElement 节点集合, 那么不为空
            ArrayList<VariableElement> variableElements = elementMap.get(activityFullName);
            if (variableElements == null){
                variableElements = new ArrayList<>();
                // 创建之后 , 将集合插入到 elementMap 集合中
                elementMap.put(activityFullName, variableElements);
            }
            // 将本节点插入到 HashSet<VariableElement> variableElements 集合中
            variableElements.add(ve);
        }

        // 生成代码
        // 遍历 HashMap<String, HashSet<VariableElement>> elementMap 集合
        // 获取 Key 的迭代器
        Iterator<String> iterator = elementMap.keySet().iterator();

        while (iterator.hasNext()){
            // 获取 Activity 全类名
            String key = iterator.next();

            // 获取 Activity 下被注解标注的 VariableElement 注解节点
            ArrayList<VariableElement> variableElements = elementMap.get(key);

            //获取对应类的包名
            // 获取 VariableElement 的父节点 TypeElement
            TypeElement typeElement = (TypeElement) variableElements.get(0).getEnclosingElement();

            // 获取 Activity 名称
            String activitySimpleName = typeElement.getSimpleName().toString();

            // 获取包节点
            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);
            // 获取包名
            String packageName = packageElement.getQualifiedName().toString();

            // 获取类名
            String className = activitySimpleName + "_ViewBinder";

            // 写出文件的字符流
            Writer writer = null;

            // 获取到包名后 , 开始生成 Java 代码
            try {

                mMessager.printMessage(Diagnostic.Kind.NOTE, "Create Java Class Name : " + packageName + "." + className);

                // 根据 包名.类名_ViewBinder 创建 Java 文件
                JavaFileObject javaFileObject = mFiler.createSourceFile(packageName + "." + className);

                // 生成 Java 代码
                writer = javaFileObject.openWriter();

                // 生成字符串文本缓冲区
                StringBuffer stringBuffer = new StringBuffer();
                // 逐行写入文本到缓冲区中

                // package kim.hsl.apt;
                stringBuffer.append("package " + packageName +";\n");

                // import android.view.View;
                stringBuffer.append("import android.view.View;\n");

                // public class MainActivity_ViewBinding implements IButterKnife<kim.hsl.apt.MainActivity>{
                stringBuffer.append("public class " + className + " implements IButterKnife<" + packageName + "." + activitySimpleName +">{\n");
                //stringBuffer.append("public class " + className +"{\n");

                // public void bind(kim.hsl.apt.MainActivity target){
                stringBuffer.append("public void bind(" + packageName + "." + activitySimpleName + " target){\n");

                for (VariableElement variableElement : variableElements){
                    // 循环被注解的字段
                    // 为每个 VariableElement 注解字段生成 target.findViewById(R.id.xxx); 代码

                    // 获取成员变量名
                    String variableName = variableElement.getSimpleName().toString();
                    // 获取资源 id , 通过注解 , 获取注解属性 value
                    int resourceId = variableElement.getAnnotation(BindView.class).value();

                    // target.
                    stringBuffer.append("target." + variableName + " = target.findViewById(" + resourceId + ");\n");
                }


                // }
                stringBuffer.append("}\n");

                // }
                stringBuffer.append("}\n");

                mMessager.printMessage(Diagnostic.Kind.NOTE, "stringBuffer.toString() : " + stringBuffer.toString());

                mMessager.printMessage(Diagnostic.Kind.NOTE, "writer : " + writer);

                        // 将字符串缓冲区的数据写出到 Java 文件中
                writer.write(stringBuffer.toString());

                mMessager.printMessage(Diagnostic.Kind.NOTE,"write finished");


            } catch (Exception e) {
                mMessager.printMessage(Diagnostic.Kind.NOTE,"IOException");
                e.printStackTrace();
            }finally {
                if (writer != null){
                    try {
                        mMessager.printMessage(Diagnostic.Kind.NOTE,"write closed");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        mMessager.printMessage(Diagnostic.Kind.NOTE,"process finished");

        return false;
    }
}