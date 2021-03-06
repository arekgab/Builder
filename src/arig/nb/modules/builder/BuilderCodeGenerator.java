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
package arig.nb.modules.builder;

import arig.nb.modules.builder.logic.ClassBuilder;
import arig.nb.modules.builder.utils.Util;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Lookup;

/**
 *
 * @author Arkadiusz Gabiga
 */
public class BuilderCodeGenerator implements CodeGenerator {

    JTextComponent textComp;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private BuilderCodeGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
        textComp = context.lookup(JTextComponent.class);
    }

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new BuilderCodeGenerator(context));
        }
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    public String getDisplayName() {
        return "Generate builder";
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    public void invoke() {
        try {
            final Util util = new Util();
            Document doc = textComp.getDocument();
            JavaSource javaSource = JavaSource.forDocument(doc);
            CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>() {
                public void run(WorkingCopy workingCopy) throws IOException {
                    workingCopy.toPhase(Phase.RESOLVED);
                    CompilationUnitTree cut = workingCopy.getCompilationUnit();
                    TreeMaker make = workingCopy.getTreeMaker();

                    Map<String, Boolean> syntheticConstructors = new HashMap<String, Boolean>();

                    List<? extends TypeElement> topLevelElements = workingCopy.getTopLevelElements();
                    for (TypeElement el : topLevelElements) {
                        if (el.getKind() == ElementKind.CLASS) {
                            List<? extends Element> enclosedElements = el.getEnclosedElements();
                            for (Element element : enclosedElements) {
                                if (element.getKind() == ElementKind.CONSTRUCTOR) {
                                    if (workingCopy.getElementUtilities().isSynthetic(element)) {
                                        syntheticConstructors.put(el.getSimpleName().toString(), Boolean.TRUE);
                                    }
                                }
                            }
                        }
                    }

                    for (Tree typeDecl : cut.getTypeDecls()) {
                        if (Tree.Kind.CLASS == typeDecl.getKind()) {
                            ClassTree clazz = (ClassTree) typeDecl;
                            List<TypeParameterTree> params = new ArrayList<TypeParameterTree>(clazz.getTypeParameters());
                            List<VariableTree> fields = new ArrayList<VariableTree>();
                            List<? extends Tree> members = clazz.getMembers();
                            boolean isSyntheticConstructor = syntheticConstructors.containsKey(clazz.getSimpleName().toString());
                            boolean hasEmptyConstructor = false;
                            for (Tree memTree : members) {
                                if (Tree.Kind.VARIABLE == memTree.getKind()) {
                                    VariableTree var = (VariableTree) memTree;
                                    if(util.canProcess(var)) {
                                        fields.add(var);
                                    }
                                }

                                if (memTree.getKind() == Tree.Kind.METHOD) {
                                    MethodTree mt = (MethodTree) memTree;
                                    if (isEmptyConstructor(mt) && isSyntheticConstructor) {
                                        hasEmptyConstructor = false;
                                    } else if (isEmptyConstructor(mt) && !isSyntheticConstructor) {
                                        hasEmptyConstructor = true; 
                                    }

                                }
                            }

                            ClassBuilder cb = new ClassBuilder(make, clazz.getSimpleName().toString(), "with", fields, params);
//                            ClassBuilder cb = new ClassBuilder(make, clazz.getSimpleName().toString(), "", fields, params);
                            ClassTree builderClass = cb.buildClass();

                            Tree builderMethod = cb.createBuilderMethod();
                            Tree constructor = cb.createConstructor();

                            ClassTree modifiedClazz = null;

                            if (!hasEmptyConstructor) {
                                Tree emptyConstructor = cb.createEmptyConstructor();
                                modifiedClazz = make.addClassMember(clazz, emptyConstructor);
                                modifiedClazz = make.addClassMember(modifiedClazz, constructor);
                                modifiedClazz = make.addClassMember(modifiedClazz, builderMethod);
                                modifiedClazz = make.addClassMember(modifiedClazz, builderClass);
                            } else {
                                modifiedClazz = make.addClassMember(clazz, constructor);
                                modifiedClazz = make.addClassMember(modifiedClazz, builderMethod);
                                modifiedClazz = make.addClassMember(modifiedClazz, builderClass);
                            }

                            workingCopy.rewrite(clazz, modifiedClazz);
                        }
                    }
                }

                public void cancel() {
                }
            };
            ModificationResult result = javaSource.runModificationTask(task);
            result.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isEmptyConstructor(MethodTree mt) {
        return mt.getName().toString().equals("<init>") 
                && mt.getReturnType() == null
                && mt.getParameters().isEmpty();
    }
}
