package com.zergatul.mixin;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Locals;

import java.util.Optional;

public class CancellableLiteInjectInjector extends Injector {

    public CancellableLiteInjectInjector(InjectionInfo info) {
        super(info, "@CancellableLiteInject");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        checkTargetModifiers(target, false);

        if (target.returnType.equals(Type.VOID_TYPE)) {
            // if target method returns void, we expect boolean
            if (!this.returnType.equals(Type.BOOLEAN_TYPE)) {
                throw new InvalidInjectionException(this.info, "@CancellableLiteInject method should return boolean.");
            }
        } else {
            throw new InvalidInjectionException(this.info, "@CancellableLiteInject currently supports only void target methods.");
            // if target method returns non-void, we expect Optional
//            if (!this.returnType.equals(Type.getType(Optional.class))) {
//                throw new InvalidInjectionException(this.info, "@CancellableLiteInject method should return Optional<T>.");
//            }
        }

        InsnList instructions = new InsnList();
        Target.Extension extraLocals = target.extendLocals();

        // push 'this'
        if (!this.isStatic) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            target.method.maxStack++;
        }

        if (this.methodArgs.length > 0) {
            LocalVariableNode[] locals = Locals.getLocalsAt(target.classNode, target.method, node.getOriginalTarget(), Locals.Settings.DEFAULT);

            for (int i = 0; i < this.methodArgs.length; i++) {
                AnnotationNode annotation = Annotations.getVisibleParameter(this.methodNode, LocalVariable.class, i);
                if (annotation == null) {
                    throw new InvalidInjectionException(info, "@LocalVariable annotation not found");
                }

                int ordinal = Annotations.<Integer>getValue(annotation, "ordinal", -1);
                int counter = 0;
                boolean found = false;
                for (int j = 0; j < locals.length; j++) {
                    LocalVariableNode local = locals[j];
                    if (local != null && local.desc != null && local.desc.equals(methodArgs[i].getDescriptor())) {
                        if (counter == ordinal) {
                            instructions.add(new VarInsnNode(this.methodArgs[i].getOpcode(Opcodes.ILOAD), local.index));
                            extraLocals.add(methodArgs[i].getSize());
                            found = true;
                            break;
                        }
                        counter++;
                    }
                }

                if (!found) {
                    throw new InvalidInjectionException(
                            info,
                            "@CancellableLiteInject injector cannot find local variable for type " + methodArgs[i] + " and ordinal " + ordinal);
                }
            }
        }

        invokeHandler(instructions);

        if (target.returnType.equals(Type.VOID_TYPE)) {
            target.method.maxStack++; // return boolean value from injector

            // if returned value is true, we return from target
            LabelNode continueNode = new LabelNode(new Label());
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, continueNode));
            instructions.add(new InsnNode(Opcodes.RETURN));
            instructions.add(continueNode);
        } else {
            throw new UnsupportedOperationException();

//            target.method.maxStack += 2; // return Optional<T> value from injector, duplicated
//            LabelNode continueNode = new LabelNode(new Label());
//            instructions.add(continueNode);
        }

        target.insns.insertBefore(node.getCurrentTarget(), instructions);
        extraLocals.apply();
    }
}