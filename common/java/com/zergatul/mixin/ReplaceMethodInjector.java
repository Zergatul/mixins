package com.zergatul.mixin;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.HashMap;
import java.util.Map;

public class ReplaceMethodInjector extends Injector {

    public ReplaceMethodInjector(InjectionInfo info) {
        super(info, "@ReplaceMethod");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode injectionNode) {
        target.insns.clear();
        target.insns.insert(cloneInstructions(this.methodNode.instructions));

        target.method.maxStack  = Math.max(target.method.maxStack,  this.methodNode.maxStack);
        target.method.maxLocals = Math.max(target.method.maxLocals, this.methodNode.maxLocals);
    }

    private InsnList cloneInstructions(InsnList instructions) {
        InsnList cloned = new InsnList();

        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode inst : instructions) {
            if (inst instanceof LabelNode labelNode) {
                labels.put(labelNode, new LabelNode(new Label()));
            }
        }

        for (AbstractInsnNode inst : instructions) {
            cloned.add(inst.clone(labels));
        }

        return cloned;
    }
}