package com.zergatul.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.invoke.InvokeInjector;
import org.spongepowered.asm.mixin.injection.struct.ArgOffsets;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Locals;

import java.util.List;

public class ModifyArgumentInjector extends InvokeInjector {

    /**
     * Index of the target arg or -1 to find the arg automatically
     * (only works where there is only one arg of specified type on the element)
     */
    private final int index;

    public ModifyArgumentInjector(InjectionInfo info, int index) {
        super(info, "@ModifyArgument");
        this.index = index;
    }

    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);

        if (!methodArgs[0].equals(returnType)) {
            throw new InvalidInjectionException(
                    info,
                    "@ModifyArgument return type on " + this + " must match the parameter type. ARG=" + methodArgs[0] + " RETURN=" + returnType);
        }
    }

    @Override
    protected void checkTarget(Target target) {
        if (!isStatic && target.isStatic) {
            throw new InvalidInjectionException(info, "non-static callback method " + this + " targets a static method which is not supported");
        }
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        checkTargetForNode(target, node, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);
        super.inject(target, node);
    }

    @Override
    protected void injectAtInvoke(Target target, InjectionNodes.InjectionNode node) {
        MethodInsnNode methodNode = (MethodInsnNode)node.getCurrentTarget();
        Type[] args = Type.getArgumentTypes(methodNode.desc);
        ArgOffsets offsets = node.getDecoration(ArgOffsets.KEY, ArgOffsets.DEFAULT);
        boolean nested = node.hasDecoration(ArgOffsets.KEY);
        Type[] originalArgs = offsets.apply(args);

        if (originalArgs.length == 0) {
            throw new InvalidInjectionException(info, "@ModifyArgument injector " + this + " targets a method invocation "
                    + ((MethodInsnNode)node.getOriginalTarget()).name + "()" + Type.getReturnType(methodNode.desc) + " with no arguments!");
        }

        int argIndex = offsets.getArgIndex(findArgIndex(target, originalArgs));

        InsnList insns = new InsnList();
        Target.Extension extraLocals = target.extendLocals();

        /**/
        int[] extraArguments = new int[methodArgs.length - 1];
        LocalVariableNode[] locals = Locals.getLocalsAt(target.classNode, target.method, node.getOriginalTarget());
        for (int i = 1; i < methodArgs.length; i++) {
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
                        extraArguments[i - 1] = locals[j].index;
                        found = true;
                        break;
                    }
                    counter++;
                }
            }

            if (!found) {
                throw new InvalidInjectionException(
                        info,
                        "@ModifyArgument injector cannot find local variable for type " + methodArgs[i] + " and ordinal " + ordinal);
            }
        }
        /**/

        injectSingleArgHandler(target, extraLocals, args, argIndex, insns, nested, extraArguments);

        target.insns.insertBefore(methodNode, insns);
        target.extendStack().set(2 - (extraLocals.get() - 1)).apply();
        extraLocals.apply();
    }

    private void injectSingleArgHandler(Target target, Target.Extension extraLocals, Type[] args, int argIndex, InsnList insns, boolean nested, int[] extraArguments) {
        int[] argMap = target.generateArgMap(args, argIndex, nested);
        storeArgs(target, args, insns, argMap, argIndex, args.length, null, null);

        //invokeHandlerWithArgs(args, insns, argMap, argIndex, argIndex + 1);
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        // push original arg
        this.pushArgs(args, insns, argMap, argIndex, argIndex + 1);
        // push @LocalVariable's
        for (int i = 0; i < extraArguments.length; i++) {
            insns.add(new VarInsnNode(methodArgs[i + 1].getOpcode(Opcodes.ILOAD), extraArguments[i]));
            extraLocals.add(methodArgs[i + 1].getSize());
        }
        this.invokeHandler(insns);

        pushArgs(args, insns, argMap, argIndex + 1, args.length);
        extraLocals.add((argMap[argMap.length - 1] - target.getMaxLocals()) + args[args.length - 1].getSize());
    }

    private int findArgIndex(Target target, Type[] args) {
        if (index > -1) {
            if (index >= args.length || !args[index].equals(returnType)) {
                throw new InvalidInjectionException(info, "Specified index " + index + " for @ModifyArgument is invalid for args "
                        + Bytecode.getDescriptor(args) + ", expected " + returnType + " on " + this);
            }
            return index;
        }

        int argIndex = -1;

        for (int arg = 0; arg < args.length; arg++) {
            if (!args[arg].equals(returnType)) {
                continue;
            }

            if (argIndex != -1) {
                throw new InvalidInjectionException(info, "Found duplicate args with index [" + argIndex + ", " + arg + "] matching type "
                        + returnType + " for @ModifyArgument target " + target + " in " + this + ". Please specify index of desired arg.");
            }

            argIndex = arg;
        }

        if (argIndex == -1) {
            throw new InvalidInjectionException(info, "Could not find arg matching type " + returnType + " for @ModifyArgument target "
                    + target + " in " + this);
        }

        return argIndex;
    }
}