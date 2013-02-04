/* 
 * Copyright (c) 2013, Arkadiusz Gabiga
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Arkadiusz Gabiga nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ARKADIUSZ GABIGA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
