package com.zergatul.mixin;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(ExecuteAfterIfElseCondition.class)
@InjectionInfo.HandlerPrefix("exec-after-if-else")
public class ExecuteAfterIfElseConditionInjectionInfo extends InjectionInfo {

    public ExecuteAfterIfElseConditionInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode annotationNode) {
        return new ExecuteAfterIfElseConditionInjector(this);
    }
}