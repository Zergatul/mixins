package com.zergatul.mixin.helpers;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class LocalVarHelper {

    public static int getStoreInst(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return ISTORE;

            case Type.LONG:
                return LSTORE;

            case Type.FLOAT:
                return FSTORE;

            case Type.DOUBLE:
                return DSTORE;

            case Type.ARRAY:
            case Type.OBJECT:
                return ASTORE;

            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static int getLoadInst(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return ILOAD;

            case Type.LONG:
                return LLOAD;

            case Type.FLOAT:
                return FLOAD;

            case Type.DOUBLE:
                return DLOAD;

            case Type.ARRAY:
            case Type.OBJECT:
                return ALOAD;

            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}