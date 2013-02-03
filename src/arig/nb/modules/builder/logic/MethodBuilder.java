/* 
 * This software is released as-is for the general good. It may be used without 
 * permission for any reason as long as my name is contained in the documentation 
 * somewhere. I don't care if it's highly visible or not. 
 * I claim no responsibility for damages that may occur to any person, place, 
 * thing, idea or noun through the direct or indirect use, misuse or 
 * disuse of this product
 */
package arig.nb.modules.builder.logic;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.netbeans.api.java.source.TreeMaker;

/**
 *
 * @author Arkadiusz Gabiga
 */
public class MethodBuilder {

    public static final String BUILDER_SETTER_METHOD_BODY = "{ this.%s = %s; return this; }";
    public static final String BUILDER_METHOD_NAME = "builder";
    public static final String BUILDER_METHOD_BODY = "{ return new %s%s(); }";
    public static final String CONSTRUCT_OBJECT = "{%s obj = new %s();\n";
    public static final String ASSIGN_TO_OBJECT = "obj.%s = this.%s;\n";
    public static final String BUILD_METHOD_NAME = "build";
    public static final String BUILD_METHOD_BODY = "{ return new %s(this);}";
    private String prefix;
    private TreeMaker make;
    private List<VariableTree> fields;

    public MethodBuilder(String prefix, TreeMaker make, List<VariableTree> fields) {
        this.prefix = prefix;
        this.make = make;
        this.fields = fields;
    }

    public List<Tree> setterMethods() {
        List<Tree> methods = new ArrayList<Tree>();
        for (VariableTree var : fields) {
            if (canProcess(var)) {
                methods.add(prepareMethod(var));
            }
        }
        return methods;
    }

    public Tree createBuilderMethod(List<? extends TypeParameterTree> parameters) {      
        String paramatrized = buildParams(parameters);
        MethodTree mt = make.Method(make.Modifiers(modifiers(Modifier.PUBLIC, Modifier.STATIC)),
                BUILDER_METHOD_NAME,
                make.Type(ClassBuilder.BUILDER_CLASS_NAME + paramatrized),
                parameters,
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                String.format(BUILDER_METHOD_BODY, ClassBuilder.BUILDER_CLASS_NAME, paramatrized),
                null);
        
        return mt;
    }

    public Tree createBuildMethod(String className) {
        MethodTree mt = make.Method(make.Modifiers(modifiers(Modifier.PUBLIC)),
                BUILD_METHOD_NAME,
                make.Type(className),
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                String.format(BUILD_METHOD_BODY, className),
                null);
        return mt;
    }
    
//    public Tree createBuildMethod(String className) {
//        MethodTree mt = make.Method(make.Modifiers(modifiers(Modifier.PUBLIC)),
//                BUILD_METHOD_NAME,
//                make.Type(className),
//                Collections.<TypeParameterTree>emptyList(),
//                Collections.<VariableTree>emptyList(),
//                Collections.<ExpressionTree>emptyList(),
//                makeBuildMethodBody(fields, className),
//                null);
//        return mt;
//    }

    protected String buildParams(List<? extends TypeParameterTree> parameters) {
        String prms = "<";
        for (TypeParameterTree parameterTree : parameters) {
            prms += parameterTree.getName().toString() + ",";
        }
        if(prms.length() > 1) {
            prms = prms.substring(0, prms.length() - 1) + ">";
        } else {
            prms = "";
        }
        return prms;
    }
    
    private boolean canProcess(VariableTree var) {
        Set<Modifier> flags = var.getModifiers().getFlags();
        return !flags.contains(Modifier.STATIC);
    }

    private Set<Modifier> modifiers(Modifier... mods) {
        return new HashSet<Modifier>(Arrays.asList(mods));
    }

    private String resolveMethodName(String fieldName) {
        String methodName = null;
        if(prefix != null && !prefix.isEmpty()){
            methodName = prefix + fieldName.subSequence(0, 1).toString().toUpperCase();
            methodName += fieldName.substring(1);
        } else {
            methodName = fieldName;
        }
        return methodName;
    }
    
    private MethodTree prepareMethod(VariableTree var) {
        ModifiersTree modifiers = make.Modifiers(modifiers(Modifier.PUBLIC));
        Tree type = var.getType();
        String fieldName = var.getName().toString();
        String methodName = resolveMethodName(fieldName);

        VariableTree parameter = make.Variable(make.Modifiers(modifiers()),
                fieldName,
                type,
                null);

        MethodTree newMethod =
                make.Method(modifiers,
                methodName,
                make.Type(ClassBuilder.BUILDER_CLASS_NAME),
                Collections.<TypeParameterTree>emptyList(),
                Collections.singletonList(parameter),
                Collections.<ExpressionTree>emptyList(),
                String.format(BUILDER_SETTER_METHOD_BODY, fieldName, fieldName),
                null);

        return newMethod;
    }

    private String makeBuildMethodBody(List<VariableTree> newMembers, String className) {
        String construct = CONSTRUCT_OBJECT;
        String assignment = ASSIGN_TO_OBJECT;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(construct, className, className));
        for (VariableTree vt : newMembers) {
            if(canProcess(vt)) {
                sb.append(String.format(assignment, vt.getName().toString(), vt.getName().toString()));
            }
        }
        sb.append("return obj;}");

        return sb.toString();
    }
}
