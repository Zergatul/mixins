package com.zergatul.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;

public class ExecuteAfterIfElseConditionInjector extends Injector {

    public ExecuteAfterIfElseConditionInjector(InjectionInfo info) {
        super(info, "@ExecuteAfterIfElseCondition");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode injectionNode) {
        checkTargetModifiers(target, false);

        AbstractInsnNode instNode = injectionNode.getCurrentTarget(); // this should be end of expression
        AbstractInsnNode nextInstNode = instNode.getNext(); // this should be jump-instruction
        if (!(nextInstNode instanceof JumpInsnNode elseBlockJump)) {
            throw new InvalidInjectionException(this.info, "@ExecuteAfterIfElseCondition should point to expression inside if statement.");
        }

        LabelNode labelNode = elseBlockJump.label; // follow the label instruction, this is jump to else block
        AbstractInsnNode prevNode = labelNode.getPrevious(); // this should be goto the end of if-else statement
        if (!(prevNode instanceof JumpInsnNode statementEndJump)) {
            throw new InvalidInjectionException(this.info, "@ExecuteAfterIfElseCondition cannot find end of if-else statement.");
        }

        InsnList instructions = new InsnList();
        if (!this.isStatic) {
            target.method.maxStack++;
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        invokeHandler(instructions);

        target.insns.insert(statementEndJump.label, instructions);
    }
}