package dev.luxury.modules.impl.killaura.rotate.mods;


import dev.luxury.Luxury;
import dev.luxury.modules.impl.killaura.rotate.deeplearnig.MinaraiModel;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.DeltaRotate;
import dev.luxury.modules.impl.killaura.rotate.mods.api.RotationMode;
import dev.luxury.modules.impl.killaura.rotate.mods.config.AiConfig;

public class AiMode extends RotationMode {
    private Rotate lerpTargetRotate = Rotate.ZERO;
    public Rotate process(AiConfig config, Rotate targetRotate) {

        DeltaRotate prevDelta = Luxury.getInstance().getRotationManager().getPreviousRotate().rotationDeltaTo(Luxury.getInstance().getRotationManager().getCurrentRotate());

        Rotate currentRotate = Luxury.getInstance().getRotationManager().getCurrentRotate();
        if(Math.abs(targetRotate.rotationDeltaTo(lerpTargetRotate).getDeltaYaw())>80 ){
            lerpTargetRotate = targetRotate;
        }
        for (int i = 0; i < 3; i++) {
            Rotate newOut = process(config, currentRotate, targetRotate, prevDelta, i == config.getTick() - 1);
            prevDelta = currentRotate.rotationDeltaTo(newOut);
            currentRotate = newOut;
        }

        if(currentRotate.rotationDeltaTo(lerpTargetRotate).isInRange(10)){
            lerpTargetRotate = targetRotate;
        }

        return currentRotate;
    }
    private Rotate process(AiConfig config, Rotate currentRotate, Rotate targetRotate, DeltaRotate prevDelta, boolean tickUpdate) {


        MinaraiModel model = Luxury.getInstance().getDeepLearningManager().getSlowModel();
        try {

            DeltaRotate deltaLerpTarget = currentRotate.rotationDeltaTo(lerpTargetRotate);


            if(Math.copySign(1,prevDelta.getDeltaYaw())!=Math.copySign(1, deltaLerpTarget.getDeltaYaw())) {
              //  prevDelta = new RotationDelta(prevDelta.getDeltaYaw()*0.2f,prevDelta.getDeltaPitch()*0.2f);
              //   prevDelta = new RotationDelta(MathUtil.lerp(prevDelta.getDeltaYaw(),deltaLerpTarget.getDeltaYaw(),0.1f),MathUtil.lerp(prevDelta.getDeltaPitch(),deltaLerpTarget.getDeltaPitch(),0.1f));
            }


            //float[] input = new float[]{prevDeltaYaw2, prevDeltaPitch2, prevDeltaYaw, prevDeltaPitch, prevTargetDiffYaw, prevTargetDiffPitch, diffa, diffb};
            float[] input = new float[]{prevDelta.getDeltaYaw(), prevDelta.getDeltaPitch(), deltaLerpTarget.getDeltaYaw(), deltaLerpTarget.getDeltaPitch()};

          //  prevTargetDelta = deltaLerpTarget;
            float[] result = model.predict(input);



            float diffYaw = result[0];
            float diffPitch = result[1];

            DeltaRotate newDelta = new DeltaRotate(diffYaw, diffPitch);



            return  currentRotate.add(newDelta);

        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return currentRotate;
    }

    public void resetLerp(Rotate targetRotate) {
        this.lerpTargetRotate = targetRotate;
    }
}
