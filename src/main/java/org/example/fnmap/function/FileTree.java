package org.example.fnmap.function;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.lang.Language;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

public class FileTree {
    private Project project;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;

    public FileTree(Project project) {
        this.project = project;
        this.rootNode = new DefaultMutableTreeNode("代码结构");
        this.treeModel = new DefaultTreeModel(rootNode);
    }

    /**
     * 获取当前活动编辑器中的文件结构树
     * @return JTree 代码结构树组件
     */
    public JTree getCurrentFileStructureTree() {
        // 清空之前的内容
        rootNode.removeAllChildren();

        // 获取当前活动的编辑器
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            rootNode.add(new DefaultMutableTreeNode("没有打开的文件"));
            treeModel.reload();
            return createTree();
        }

        // 获取当前文件
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        if (selectedFiles.length == 0) {
            rootNode.add(new DefaultMutableTreeNode("无法获取文件信息"));
            treeModel.reload();
            return createTree();
        }
        
        VirtualFile virtualFile = selectedFiles[0];

        // 获取PSI文件
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            rootNode.add(new DefaultMutableTreeNode("无法解析文件"));
            treeModel.reload();
            return createTree();
        }

        // 检测语言并设置根节点名称
        Language language = psiFile.getLanguage();
        String fileName = virtualFile.getName();
        String languageInfo = getLanguageDisplayName(language, virtualFile);
        rootNode.setUserObject(fileName + " (" + languageInfo + ")");

        // 根据语言类型构建结构树
        buildFileStructure(psiFile, rootNode);

        treeModel.reload();
        return createTree();
    }

    /**
     * 获取语言显示名称
     */
    private String getLanguageDisplayName(Language language, VirtualFile file) {
        String extension = file.getExtension();
        String languageName = language.getDisplayName();
        
        // 根据文件扩展名和语言信息返回更友好的显示名称
        if ("JAVA".equalsIgnoreCase(languageName)) {
            return "Java";
        } else if ("JavaScript".equalsIgnoreCase(languageName)) {
            return "JavaScript";
        } else if ("TypeScript".equalsIgnoreCase(languageName)) {
            return "TypeScript";
        } else if ("Python".equalsIgnoreCase(languageName)) {
            return "Python";
        } else if ("kotlin".equalsIgnoreCase(languageName)) {
            return "Kotlin";
        } else if ("XML".equalsIgnoreCase(languageName)) {
            return "XML";
        } else if ("JSON".equalsIgnoreCase(languageName)) {
            return "JSON";
        } else if ("TEXT".equalsIgnoreCase(languageName)) {
            if ("md".equalsIgnoreCase(extension)) return "Markdown";
            if ("txt".equalsIgnoreCase(extension)) return "Text";
            return "Plain Text";
        } else {
            return languageName + (extension != null ? " (." + extension + ")" : "");
        }
    }

    /**
     * 构建文件结构
     */
    private void buildFileStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        Language language = psiFile.getLanguage();
        
        if (psiFile instanceof PsiJavaFile) {
            buildJavaFileStructure((PsiJavaFile) psiFile, parentNode);
        } else if (language.getID().equals("kotlin")) {
            buildKotlinFileStructure(psiFile, parentNode);
        } else if (language.getID().equals("JavaScript") || language.getID().equals("TypeScript")) {
            buildJavaScriptFileStructure(psiFile, parentNode);
        } else if (language.getID().equals("Python")) {
            buildPythonFileStructure(psiFile, parentNode);
        } else if (psiFile instanceof XmlFile) {
            buildXmlFileStructure((XmlFile) psiFile, parentNode);
        } else if (psiFile instanceof PsiPlainTextFile) {
            buildPlainTextStructure(psiFile, parentNode);
        } else {
            buildGenericFileStructure(psiFile, parentNode);
        }
    }

    /**
     * 构建Java文件结构
     */
    private void buildJavaFileStructure(PsiJavaFile javaFile, DefaultMutableTreeNode parentNode) {
        // 添加包信息
        String packageName = javaFile.getPackageName();
        if (!packageName.isEmpty()) {
            DefaultMutableTreeNode packageNode = new DefaultMutableTreeNode(new StructureElement("📦 " + packageName, null, "package"));
            parentNode.add(packageNode);
        }

        // 添加导入信息
        PsiImportList importList = javaFile.getImportList();
        if (importList != null && importList.getChildren().length > 0) {
            DefaultMutableTreeNode importsNode = new DefaultMutableTreeNode(new StructureElement("📋 导入 (" + importList.getChildren().length + ")", null, "imports"));
            parentNode.add(importsNode);

            for (PsiImportStatement importStatement : importList.getImportStatements()) {
                String importText = importStatement.getText().replace("import ", "").replace(";", "");
                importsNode.add(new DefaultMutableTreeNode(new StructureElement("📄 " + importText, importStatement, "import")));
            }
        }

        // 添加类信息
        PsiClass[] classes = javaFile.getClasses();
        for (PsiClass psiClass : classes) {
            addClassNode(psiClass, parentNode);
        }
    }

    /**
     * 构建Kotlin文件结构
     */
    private void buildKotlinFileStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        // 使用通用方法查找类和函数
        Collection<PsiElement> classes = PsiTreeUtil.findChildrenOfAnyType(psiFile, false, PsiClass.class);
        Collection<PsiElement> functions = PsiTreeUtil.findChildrenOfAnyType(psiFile, false, PsiMethod.class);
        
        if (!classes.isEmpty()) {
            DefaultMutableTreeNode classesNode = new DefaultMutableTreeNode(new StructureElement("🏛️ 类 (" + classes.size() + ")", null, "classes"));
            parentNode.add(classesNode);
            
            for (PsiElement element : classes) {
                if (element instanceof PsiClass) {
                    addClassNode((PsiClass) element, classesNode);
                }
            }
        }
        
        if (!functions.isEmpty()) {
            DefaultMutableTreeNode functionsNode = new DefaultMutableTreeNode(new StructureElement("⚙️ 函数 (" + functions.size() + ")", null, "functions"));
            parentNode.add(functionsNode);
            
            for (PsiElement element : functions) {
                if (element instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) element;
                    String methodInfo = getMethodInfo(method);
                    functionsNode.add(new DefaultMutableTreeNode(new StructureElement("🔹 " + methodInfo, method, "function")));
                }
            }
        }
    }

    /**
     * 构建JavaScript/TypeScript文件结构
     */
    private void buildJavaScriptFileStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        // 对于JS/TS文件，查找函数声明
        Collection<PsiElement> functions = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement.class);
        int functionCount = 0;
        DefaultMutableTreeNode functionsNode = new DefaultMutableTreeNode(new StructureElement("⚙️ 函数", null, "functions"));
        
        for (PsiElement element : functions) {
            String elementText = element.getText();
            if (elementText.startsWith("function ") || elementText.contains("=>") || elementText.contains("function(")) {
                functionCount++;
                String functionName = extractFunctionName(elementText);
                functionsNode.add(new DefaultMutableTreeNode(new StructureElement("🔹 " + functionName, element, "function")));
            }
        }
        
        if (functionCount > 0) {
            functionsNode.setUserObject(new StructureElement("⚙️ 函数 (" + functionCount + ")", null, "functions"));
            parentNode.add(functionsNode);
        }
    }

    /**
     * 构建Python文件结构
     */
    private void buildPythonFileStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        // 对于Python文件，查找类和函数定义
        String content = psiFile.getText();
        String[] lines = content.split("\n");
        
        DefaultMutableTreeNode classesNode = new DefaultMutableTreeNode(new StructureElement("🏛️ 类", null, "classes"));
        DefaultMutableTreeNode functionsNode = new DefaultMutableTreeNode(new StructureElement("⚙️ 函数", null, "functions"));
        
        int classCount = 0;
        int functionCount = 0;
        int lineNumber = 0;
        
        for (String line : lines) {
            lineNumber++;
            String trimmedLine = line.trim();
            
            if (trimmedLine.startsWith("class ")) {
                classCount++;
                String className = extractPythonClassName(trimmedLine);
                classesNode.add(new DefaultMutableTreeNode(new StructureElement("🏛️ " + className, null, "class", lineNumber)));
            } else if (trimmedLine.startsWith("def ")) {
                functionCount++;
                String functionName = extractPythonFunctionName(trimmedLine);
                functionsNode.add(new DefaultMutableTreeNode(new StructureElement("🔹 " + functionName, null, "function", lineNumber)));
            }
        }
        
        if (classCount > 0) {
            classesNode.setUserObject(new StructureElement("🏛️ 类 (" + classCount + ")", null, "classes"));
            parentNode.add(classesNode);
        }
        
        if (functionCount > 0) {
            functionsNode.setUserObject(new StructureElement("⚙️ 函数 (" + functionCount + ")", null, "functions"));
            parentNode.add(functionsNode);
        }
    }

    /**
     * 构建XML文件结构
     */
    private void buildXmlFileStructure(XmlFile xmlFile, DefaultMutableTreeNode parentNode) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            addXmlTagNode(rootTag, parentNode);
        }
    }

    /**
     * 添加XML标签节点
     */
    private void addXmlTagNode(XmlTag tag, DefaultMutableTreeNode parentNode) {
        String tagName = tag.getName();
        String attributes = "";
        if (tag.getAttributes().length > 0) {
            attributes = " [" + tag.getAttributes().length + " 属性]";
        }
        
        DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(
            new StructureElement("🏷️ <" + tagName + ">" + attributes, tag, "xml-tag")
        );
        parentNode.add(tagNode);
        
        // 添加子标签
        XmlTag[] subTags = tag.getSubTags();
        for (XmlTag subTag : subTags) {
            addXmlTagNode(subTag, tagNode);
        }
    }

    /**
     * 添加类节点
     */
    private void addClassNode(PsiClass psiClass, DefaultMutableTreeNode parentNode) {
        String className = psiClass.getName();
        String classType = getClassType(psiClass);
        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
            new StructureElement(classType + " " + className, psiClass, "class")
        );
        parentNode.add(classNode);

        // 添加字段
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            DefaultMutableTreeNode fieldsNode = new DefaultMutableTreeNode(
                new StructureElement("🔧 字段 (" + fields.length + ")", null, "fields")
            );
            classNode.add(fieldsNode);

            for (PsiField field : fields) {
                String fieldInfo = getFieldInfo(field);
                fieldsNode.add(new DefaultMutableTreeNode(
                    new StructureElement("🔸 " + fieldInfo, field, "field")
                ));
            }
        }

        // 添加构造方法
        PsiMethod[] constructors = psiClass.getConstructors();
        if (constructors.length > 0) {
            DefaultMutableTreeNode constructorsNode = new DefaultMutableTreeNode(
                new StructureElement("🏗️ 构造方法 (" + constructors.length + ")", null, "constructors")
            );
            classNode.add(constructorsNode);

            for (PsiMethod constructor : constructors) {
                String constructorInfo = getMethodInfo(constructor);
                constructorsNode.add(new DefaultMutableTreeNode(
                    new StructureElement("🔧 " + constructorInfo, constructor, "constructor")
                ));
            }
        }

        // 添加方法
        PsiMethod[] methods = psiClass.getMethods();
        int methodCount = 0;
        for (PsiMethod method : methods) {
            if (!method.isConstructor()) {
                methodCount++;
            }
        }

        if (methodCount > 0) {
            DefaultMutableTreeNode methodsNode = new DefaultMutableTreeNode(
                new StructureElement("⚙️ 方法 (" + methodCount + ")", null, "methods")
            );
            classNode.add(methodsNode);

            for (PsiMethod method : methods) {
                if (!method.isConstructor()) {
                    String methodInfo = getMethodInfo(method);
                    methodsNode.add(new DefaultMutableTreeNode(
                        new StructureElement("🔹 " + methodInfo, method, "method")
                    ));
                }
            }
        }

        // 添加内部类
        PsiClass[] innerClasses = psiClass.getInnerClasses();
        for (PsiClass innerClass : innerClasses) {
            addClassNode(innerClass, classNode);
        }
    }

    /**
     * 提取JavaScript函数名
     */
    private String extractFunctionName(String functionText) {
        if (functionText.contains("function ")) {
            int start = functionText.indexOf("function ") + 9;
            int end = functionText.indexOf("(", start);
            if (end > start) {
                return functionText.substring(start, end).trim();
            }
        }
        return "匿名函数";
    }

    /**
     * 提取Python类名
     */
    private String extractPythonClassName(String line) {
        String className = line.substring(6); // 移除 "class "
        int colonIndex = className.indexOf(":");
        if (colonIndex > 0) {
            className = className.substring(0, colonIndex);
        }
        int parenIndex = className.indexOf("(");
        if (parenIndex > 0) {
            className = className.substring(0, parenIndex);
        }
        return className.trim();
    }

    /**
     * 提取Python函数名
     */
    private String extractPythonFunctionName(String line) {
        String functionName = line.substring(4); // 移除 "def "
        int parenIndex = functionName.indexOf("(");
        if (parenIndex > 0) {
            functionName = functionName.substring(0, parenIndex);
        }
        return functionName.trim();
    }

    /**
     * 获取类类型
     */
    private String getClassType(PsiClass psiClass) {
        if (psiClass.isInterface()) {
            return "🔷 接口";
        } else if (psiClass.isEnum()) {
            return "📋 枚举";
        } else if (psiClass.isAnnotationType()) {
            return "🏷️ 注解";
        } else {
            return "🏛️ 类";
        }
    }

    /**
     * 获取字段信息
     */
    private String getFieldInfo(PsiField field) {
        StringBuilder info = new StringBuilder();

        String modifier = getModifierString(field);
        if (!modifier.isEmpty()) {
            info.append(modifier).append(" ");
        }

        PsiType type = field.getType();
        info.append(type.getPresentableText()).append(" ");
        info.append(field.getName());

        return info.toString();
    }

    /**
     * 获取方法信息
     */
    private String getMethodInfo(PsiMethod method) {
        StringBuilder info = new StringBuilder();

        String modifier = getModifierString(method);
        if (!modifier.isEmpty()) {
            info.append(modifier).append(" ");
        }

        if (!method.isConstructor()) {
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                info.append(returnType.getPresentableText()).append(" ");
            }
        }

        info.append(method.getName());

        PsiParameterList parameterList = method.getParameterList();
        info.append("(");
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                info.append(", ");
            }
            info.append(parameters[i].getType().getPresentableText());
            info.append(" ");
            info.append(parameters[i].getName());
        }
        info.append(")");

        return info.toString();
    }

    /**
     * 获取修饰符字符串
     */
    private String getModifierString(PsiModifierListOwner element) {
        PsiModifierList modifierList = element.getModifierList();
        if (modifierList == null) {
            return "";
        }

        StringBuilder modifiers = new StringBuilder();
        if (modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
            modifiers.append("public ");
        }
        if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
            modifiers.append("private ");
        }
        if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
            modifiers.append("protected ");
        }
        if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
            modifiers.append("static ");
        }
        if (modifierList.hasModifierProperty(PsiModifier.FINAL)) {
            modifiers.append("final ");
        }
        if (modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) {
            modifiers.append("abstract ");
        }

        return modifiers.toString().trim();
    }

    /**
     * 构建纯文本文件结构
     */
    private void buildPlainTextStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        String content = psiFile.getText();
        String[] lines = content.split("\n");

        DefaultMutableTreeNode infoNode = new DefaultMutableTreeNode(
            new StructureElement("📄 文件信息", null, "info")
        );
        parentNode.add(infoNode);

        infoNode.add(new DefaultMutableTreeNode(new StructureElement("总行数: " + lines.length, null, "line-count")));
        infoNode.add(new DefaultMutableTreeNode(new StructureElement("字符数: " + content.length(), null, "char-count")));
        infoNode.add(new DefaultMutableTreeNode(new StructureElement("文件类型: 纯文本", null, "file-type")));
    }

    /**
     * 构建通用文件结构
     */
    private void buildGenericFileStructure(PsiFile psiFile, DefaultMutableTreeNode parentNode) {
        DefaultMutableTreeNode infoNode = new DefaultMutableTreeNode(
            new StructureElement("📄 文件信息", null, "info")
        );
        parentNode.add(infoNode);

        infoNode.add(new DefaultMutableTreeNode(new StructureElement("文件类型: " + psiFile.getFileType().getName(), null, "file-type")));
        infoNode.add(new DefaultMutableTreeNode(new StructureElement("语言: " + psiFile.getLanguage().getDisplayName(), null, "language")));

        String content = psiFile.getText();
        if (content != null) {
            String[] lines = content.split("\n");
            infoNode.add(new DefaultMutableTreeNode(new StructureElement("总行数: " + lines.length, null, "line-count")));
            infoNode.add(new DefaultMutableTreeNode(new StructureElement("字符数: " + content.length(), null, "char-count")));
        }
    }

    /**
     * 创建JTree组件并添加点击跳转功能
     */
    private JTree createTree() {
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        // 添加鼠标双击监听器实现跳转功能
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();
                        
                        if (userObject instanceof StructureElement) {
                            StructureElement element = (StructureElement) userObject;
                            navigateToElement(element);
                        }
                    }
                }
            }
        });

        // 展开根节点
        tree.expandRow(0);

        return tree;
    }

    /**
     * 导航到指定元素
     */
    private void navigateToElement(StructureElement element) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        PsiElement psiElement = element.getPsiElement();
        if (psiElement != null) {
            // 获取元素在文档中的偏移量
            int offset = psiElement.getTextOffset();
            
            // 将光标移动到指定位置
            editor.getCaretModel().moveToOffset(offset);
            
            // 滚动到该位置
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            
            // 请求焦点
            editor.getContentComponent().requestFocus();
        } else if (element.getLineNumber() > 0) {
            // 对于无法获取PSI元素的情况，使用行号跳转
            int lineNumber = element.getLineNumber() - 1; // 转换为0基索引
            int offset = editor.getDocument().getLineStartOffset(lineNumber);
            
            editor.getCaretModel().moveToOffset(offset);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            editor.getContentComponent().requestFocus();
        }
    }

    /**
     * 刷新结构树
     */
    public void refresh() {
        getCurrentFileStructureTree();
    }

    /**
     * 结构元素类，用于存储元素信息和PSI元素引用
     */
    private static class StructureElement {
        private final String displayText;
        private final PsiElement psiElement;
        private final String type;
        private final int lineNumber;

        public StructureElement(String displayText, PsiElement psiElement, String type) {
            this(displayText, psiElement, type, -1);
        }

        public StructureElement(String displayText, PsiElement psiElement, String type, int lineNumber) {
            this.displayText = displayText;
            this.psiElement = psiElement;
            this.type = type;
            this.lineNumber = lineNumber;
        }

        public String getDisplayText() {
            return displayText;
        }

        public PsiElement getPsiElement() {
            return psiElement;
        }

        public String getType() {
            return type;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }
}