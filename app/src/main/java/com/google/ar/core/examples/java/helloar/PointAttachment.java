package com.google.ar.core.examples.java.helloar;

import android.graphics.Point;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;

/**
 * Created by prajna on 12/9/17.
 */

public class PointAttachment {
    private final PointCloud mPoint;
    private final Anchor mAnchor;

    // Allocate temporary storage to avoid multiple allocations per frame.
    private final float[] mPoseTranslation = new float[3];
    private final float[] mPoseRotation = new float[4];

    public PointAttachment(PointCloud point, Anchor anchor) {
        mPoint = point;
        mAnchor = anchor;
    }

//    public boolean isTracking() {
//        return /*true if*/
//                mPoint.getPoints() == mAnchor.getTrackingState() == Anchor.TrackingState.TRACKING;
//    }

    public Pose getPose() {
        Pose pose = mAnchor.getPose();
        pose.getTranslation(mPoseTranslation, 0);
        pose.getRotationQuaternion(mPoseRotation, 0);
//        mPoseTranslation[1] = mPoint.getPoints().;
        return new Pose(mPoseTranslation, mPoseRotation);
    }

    public Anchor getAnchor() {
        return mAnchor;
    }
}

