/* 
 * This software is released as-is for the general good. It may be used without 
 * permission for any reason as long as my name is contained in the documentation 
 * somewhere. I don't care if it's highly visible or not. 
 * I claim no responsibility for damages that may occur to any person, place, 
 * thing, idea or noun through the direct or indirect use, misuse or 
 * disuse of this product
 */
package arig.nb.modules.builder.logic;

import arig.nb.modules.builder.utils.Util;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
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
public class ClassBuilder {

    public static final String BUILDER_CLASS_NAME = "Builder";
    public static final String CONSTRUCTOR_SETTER_LINE = "this.%s = %s.%s;";
    private TreeMaker make;
    private String className;
    private String methodPrefix;
    private List<VariableTree> fields;
    private List<Tree> allMembers;
    private MethodBuilder mb;
    private List<? extends TypeParameterTree> parameters;
    private Util util;

    public ClassBuilder(TreeMaker make, String className, String methodPrefix, List<VariableTree> fields, List<? extends TypeParameterTree> parameters) {
        this.make = make;
        this.className = className;
        this.methodPrefix = methodPrefix;
        this.fields = fields;
        this.allMembers = new ArrayList<Tree>();
        this.mb = new MethodBuilder(methodPrefix, make, fields);

        if (parameters == null) {
            parameters = Collections.<TypeParameterTree>emptyList();
        }

        this.parameters = parameters;
        this.util = new Util();
    }

    public Tree createBuilderMethod() {
        return mb.createBuilderMethod(this.parameters);
    }

    public Tree createConstructor() {
        VariableTree parameter = make.Variable(make.Modifiers(util.modifiers()),
                BUILDER_CLASS_NAME.toLowerCase(),
                make.Type(BUILDER_CLASS_NAME + mb.buildParams(parameters)),
                null);
        MethodTree constructor = make.Constructor(
                make.Modifiers(util.modifiers(Modifier.PRIVATE)),
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>singletonList(parameter),
                Collections.<ExpressionTree>emptyList(),
                buildConstructorBody());

        return constructor;
    }
    
    public Tree createEmptyConstructor() {
        MethodTree constructor = make.Constructor(
                make.Modifiers(util.modifiers(Modifier.PUBLIC)),
                Collections.<TypeParameterTree>emptyList(),
                Collections.<VariableTree>emptyList(),
                Collections.<ExpressionTree>emptyList(),
                "{}");

        return constructor;
    }
    
    

    public ClassTree buildClass() {
        ModifiersTree classModifiers = make.Modifiers(util.modifiers(Modifier.PUBLIC, Modifier.STATIC));
        List<Tree> setterMethods = mb.setterMethods();
        Tree buildMethod = mb.createBuildMethod(className);

        allMembers.addAll(Arrays.asList(fields.toArray(new Tree[]{})));
        allMembers.addAll(setterMethods);
        allMembers.add(buildMethod);

        ClassTree builderClazz = make.Class(classModifiers, BUILDER_CLASS_NAME, parameters, null, Collections.<Tree>emptyList(), allMembers);
        return builderClazz;
    }

    private String buildConstructorBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (VariableTree vt : fields) {
            if (util.canProcess(vt)) {
                sb.append(String.format(CONSTRUCTOR_SETTER_LINE,
                        vt.getName().toString(),
                        BUILDER_CLASS_NAME.toLowerCase(),
                        vt.getName().toString()));
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
