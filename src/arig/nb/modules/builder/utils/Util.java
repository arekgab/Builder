/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package arig.nb.modules.builder.utils;

import com.sun.source.tree.VariableTree;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 *
 * @author arek
 */
public class Util {
    public boolean canProcess(VariableTree var) {
        Set<Modifier> flags = var.getModifiers().getFlags();
        return !flags.contains(Modifier.STATIC);
    }
    
    public Set<Modifier> modifiers(Modifier... mods) {
        return new HashSet<Modifier>(Arrays.asList(mods));
    }
}
