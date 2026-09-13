package com.zergatul.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;

import java.util.ListIterator;

public class RethrowWithConditionInjector extends Injector {

    public RethrowWithConditionInjector(InjectionInfo info) {
        super(info, "@RethrowWithCondition");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        checkTargetModifiers(target, false);

        if (this.returnType != Type.BOOLEAN_TYPE) {
            throw new InvalidInjectionException(this.info, "@RethrowWithCondition should return boolean.");
        }
        if (this.methodArgs.length != 0) {
            throw new InvalidInjectionException(this.info, "@RethrowWithCondition should accept 0 arguments.");
        }

        // find try-block
        LabelNode targetCatch = null;
        for (TryCatchBlockNode block : target.method.tryCatchBlocks) {
            if (block.handler == null) {
                continue;
            }
            LabelNode handler = block.handler;
            LabelNode end = block.end;
            if (isBetweenLabels(target.insns, end, handler, node.getCurrentTarget())) {
                targetCatch = handler;
                break;
            }
        }

        if (targetCatch == null) {
            throw new InvalidInjectionException(this.info, "@RethrowWithCondition doesn't point to catch block.");
        }

        InsnList instructions = new InsnList();

        // push 'this'
        if (!this.isStatic) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        invokeHandler(instructions);

        LabelNode label = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, label));
        instructions.add(new InsnNode(Opcodes.ATHROW));
        instructions.add(label);

        target.insns.insert(targetCatch, instructions);
    }

    private static boolean isBetweenLabels(InsnList instList, LabelNode label1, LabelNode label2, AbstractInsnNode current) {
        boolean visitedLabel1 = false;
        boolean visitedLabel2 = false;

        ListIterator<AbstractInsnNode> iterator = instList.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode inst = iterator.next();

            if (inst == label1) {
                if (visitedLabel2) {
                    return false;
                }
                visitedLabel1 = true;
            }

            if (inst == label2) {
                if (!visitedLabel1) {
                    return false;
                }
                visitedLabel2 = true;
            }

            if (inst == current) {
                return visitedLabel1 && visitedLabel2;
            }
        }

        return false;
    }
}