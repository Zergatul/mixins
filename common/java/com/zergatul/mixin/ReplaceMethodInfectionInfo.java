package com.zergatul.mixin;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(ReplaceMethod.class)
@InjectionInfo.HandlerPrefix("replace-method")
public class ReplaceMethodInfectionInfo extends InjectionInfo {

    public ReplaceMethodInfectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode annotationNode) {
        return new ReplaceMethodInjector(this);
    }
}