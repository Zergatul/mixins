package com.zergatul.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Locals;

public class ModifyMethodReturnValueInjector extends Injector {

    public ModifyMethodReturnValueInjector(InjectionInfo info) {
        super(info, "@ModifyMethodReturnValue");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        checkTargetModifiers(target, false);

        AbstractInsnNode instructionNode = node.getCurrentTarget();
        if (instructionNode instanceof MethodInsnNode methodInsnNode) {
            Type originalReturnType = Type.getReturnType(methodInsnNode.desc);
            if (originalReturnType == Type.VOID_TYPE) {
                throw new InvalidInjectionException(this.info, "@ModifyMethodReturnValue should not point to void methods.");
            }

            if (!this.returnType.equals(originalReturnType)) {
                throw new InvalidInjectionException(this.info, "@ModifyMethodReturnValue return types should match.");
            }

            if (this.methodArgs.length == 0) {
                throw new InvalidInjectionException(this.info, "@ModifyMethodReturnValue method should accept at least 1 parameter.");
            }
            if (!this.methodArgs[0].equals(originalReturnType)) {
                throw new InvalidInjectionException(this.info, "@ModifyMethodReturnValue method should accept type " + originalReturnType + " as first parameter.");
            }

            InsnList instructions = new InsnList();
            Target.Extension extraLocals = target.extendLocals();

            int[] tempVariables = new int[this.methodArgs.length];
            // save result into variable
            tempVariables[0] = target.allocateLocals(originalReturnType.getSize());
            instructions.add(new VarInsnNode(originalReturnType.getOpcode(Opcodes.ISTORE), tempVariables[0]));
            target.addLocalVariable(tempVariables[0], "@result", originalReturnType.getDescriptor(), null, null);

            // save other parameters into variables
            if (this.methodArgs.length > 1) {
                LocalVariableNode[] locals = Locals.getLocalsAt(target.classNode, target.method, node.getOriginalTarget(), Locals.Settings.DEFAULT);

                extraLocals.add(2); // prepare stack for loading single value, which may have stacksize=2
                for (int i = 1; i < this.methodArgs.length; i++) {
                    AnnotationNode annotation = Annotations.getVisibleParameter(this.methodNode, LocalVariable.class, i);
                    if (annotation == null) {
                        throw new InvalidInjectionException(info, "@LocalVariable annotation not found");
                    }

                    int ordinal = Annotations.<Integer>getValue(annotation, "ordinal", -1);
                    int counter = 0;
                    boolean found = false;
                    for (int j = 0; j < locals.length; j++) {
                        if (locals[j].desc.equals(methodArgs[i].getDescriptor())) {
                            if (counter == ordinal) {
                                instructions.add(new VarInsnNode(this.methodArgs[i].getOpcode(Opcodes.ILOAD), locals[j].index));
                                found = true;
                                break;
                            }
                            counter++;
                        }
                    }

                    if (!found) {
                        throw new InvalidInjectionException(
                                info,
                                "@ModifyMethodReturnValue injector cannot find local variable for type " + methodArgs[i] + " and ordinal " + ordinal);
                    }

                    tempVariables[i] = target.allocateLocals(this.methodArgs[i].getSize());
                    instructions.add(new VarInsnNode(this.methodArgs[i].getOpcode(Opcodes.ISTORE), tempVariables[i]));
                    target.addLocalVariable(tempVariables[i], "@arg" + i, this.methodArgs[i].getDescriptor(), null, null);
                }
            }

            // push 'this'
            if (!this.isStatic) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                extraLocals.add(1);
            }

            // push arguments
            for (int i = 0; i < this.methodArgs.length; i++) {
                instructions.add(new VarInsnNode(this.methodArgs[i].getOpcode(Opcodes.ILOAD), tempVariables[i]));
                if (i > 0) {
                    extraLocals.add(this.methodArgs[i].getSize());
                }
            }

            invokeHandler(instructions);
            target.insns.insert(instructionNode, instructions);
            extraLocals.apply();
        } else {
            throw new InvalidInjectionPointException(this.info, "@ModifyMethodReturnValue should point to method instruction.");
        }
    }
}