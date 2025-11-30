package com.zergatul.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Locals;

public class LiteInjectInjector extends Injector {

    public LiteInjectInjector(InjectionInfo info) {
        super(info, "@LiteInject");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        checkTargetModifiers(target, false);

        if (!this.returnType.equals(Type.VOID_TYPE)) {
            throw new InvalidInjectionException(this.info, "@LiteInject method should return void.");
        }

        InsnList instructions = new InsnList();
        Target.Extension extraLocals = target.extendLocals();

        // push 'this'
        if (!this.isStatic) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            extraLocals.add(1);
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
                    if (locals[j].desc.equals(methodArgs[i].getDescriptor())) {
                        if (counter == ordinal) {
                            instructions.add(new VarInsnNode(this.methodArgs[i].getOpcode(Opcodes.ILOAD), locals[j].index));
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
                            "@LiteInject injector cannot find local variable for type " + methodArgs[i] + " and ordinal " + ordinal);
                }
            }
        }

        invokeHandler(instructions);
        target.insns.insert(node.getCurrentTarget(), instructions);
        extraLocals.apply();
    }
}