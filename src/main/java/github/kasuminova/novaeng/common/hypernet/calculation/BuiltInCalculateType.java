package github.kasuminova.novaeng.common.hypernet.calculation;

abstract sealed class BuiltInCalculateType extends CalculateType permits CalculateTypes.CalculateTypeIntricate, CalculateTypes.CalculateTypeLogic, CalculateTypes.CalculateTypeNeuron, CalculateTypes.CalculateTypeQbit {

    BuiltInCalculateType(final String typeName) {
        super(typeName);
    }

}
