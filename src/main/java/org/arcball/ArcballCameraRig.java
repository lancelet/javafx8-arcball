package org.arcball;

import org.arcball.internal.InteractionDragArcball;
import org.arcball.internal.InteractionDragPan;
import org.arcball.internal.InteractionDragZoom;
import org.arcball.internal.InteractionScrollZoom;
import org.arcball.internal.NoGarbageProperty;
import org.arcball.internal.PerspectiveSceneToRaster;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

public final class ArcballCameraRig implements CameraRig {

    //---------------------------------------------------------------------------------------------------------- PUBLIC
    
    public ArcballCameraRig() {
        buildListeners();
    }
    
    @Override public void attachToScene(Scene scene) {
        assert((this.scene == null) && (this.subscene == null));
        this.scene = scene;
        scene.setCamera(camera.get());
        scrollZoom.attachToScene(scene);
        dragZoom.attachToScene(scene);
        pan.attachToScene(scene);
        arcball.attachToScene(scene);
    }

    @Override public void detachFromScene(Scene scene) {
        assert((this.scene == scene) && (this.subscene == null));
        scrollZoom.detachFromScene(scene);
        dragZoom.detachFromScene(scene);
        pan.detachFromScene(scene);
        arcball.detachFromScene(scene);
        scene.setCamera(null);
        this.scene = null;
    }

    @Override public void attachToSubScene(SubScene subscene) {
        assert((this.scene == null) && (this.subscene == null));
        this.subscene = subscene;
        subscene.setCamera(camera.get());
        scrollZoom.attachToSubScene(subscene);
        dragZoom.attachToSubScene(subscene);
        pan.attachToSubScene(subscene);
        arcball.attachToSubScene(subscene);
    }

    @Override public void detachFromSubScene(SubScene subscene) {
        assert((this.scene == null) && (this.subscene == subscene));
        scrollZoom.detachFromSubScene(subscene);
        dragZoom.detachFromSubScene(subscene);
        pan.detachFromSubScene(subscene);
        arcball.detachFromSubScene(subscene);
        subscene.setCamera(null);
        this.subscene = null;
    }

    @Override public void encompassBounds(Bounds bounds, double animationDurationMillis) {
        // find the center of the bounds
        double cx = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double cy = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
        double cz = (bounds.getMinZ() + bounds.getMaxZ()) / 2.0;
        // find the "radius", as the maximum of the depth, height and width divided by 2
        double r = Math.max(bounds.getDepth(), Math.max(bounds.getHeight(), bounds.getWidth())) / 2.0;
        // configure camera
        if (camera.get() instanceof PerspectiveCamera) {
          PerspectiveCamera pCamera = (PerspectiveCamera)camera.get();
          double fov = pCamera.getFieldOfView() * Math.PI / 180.0;
          double d = r / Math.tan(fov / 2.0);
          if (animationDurationMillis <= 0.0) {
              distanceFromOrigin.set(1.1 * d);
              setOrigin(cx, cy, cz);
          } else {
              Duration tf = Duration.millis(animationDurationMillis);
              Timeline timeline = new Timeline();
              timeline.getKeyFrames().add(new KeyFrame(tf, new KeyValue(distanceFromOriginProperty(), 1.1 * d)));
              timeline.getKeyFrames().add(new KeyFrame(tf, new KeyValue(originXProperty(), cx)));
              timeline.getKeyFrames().add(new KeyFrame(tf, new KeyValue(originYProperty(), cy)));
              timeline.getKeyFrames().add(new KeyFrame(tf, new KeyValue(originZProperty(), cz)));
              timeline.play();
          }
          pCamera.setNearClip(0.05 * d);
          pCamera.setFarClip(10.0 * d);
        }
    }

    @Override public ReadOnlyObjectProperty<PerspectiveCamera> cameraProperty() { return camera; }

    @Override public ReadOnlyObjectProperty<CameraToRasterTransform> transformSceneToRasterProperty() {
        return transformSceneToRaster;
    }

    @Override public ReadOnlyObjectProperty<Transform> transformRotationOnlyProperty() {
        return transformRotationOnly;
    }

    @Override public ReadOnlyObjectProperty<Transform> transformRotationTranslationProperty() {
        return transformRotationTranslation;
    }

    public void setOrigin(double x, double y, double z) {
        originX.set(x);
        originY.set(y);
        originZ.set(z);
    }
    
    public DoubleProperty originXProperty() { return originX; }
    
    public DoubleProperty originYProperty() { return originY; }
    
    public DoubleProperty originZProperty() { return originZ; }
    
    public ReadOnlyDoubleProperty rotationAxisXProperty() { return rotationAxisX; }
    
    public ReadOnlyDoubleProperty rotationAxisYProperty() { return rotationAxisY; }
    
    public ReadOnlyDoubleProperty rotationAxisZProperty() { return rotationAxisZ; }
    
    public ReadOnlyDoubleProperty rotationAngleProperty() { return rotationAngle; }
    
    public DoubleProperty distanceFromOriginProperty() { return distanceFromOrigin; }
    
    //--------------------------------------------------------------------------------------------------------- PRIVATE
    
    private final DoubleProperty originX = new SimpleDoubleProperty(this, "originX", 0);
    private final DoubleProperty originY = new SimpleDoubleProperty(this, "originY", 0);
    private final DoubleProperty originZ = new SimpleDoubleProperty(this, "originZ", 0);
    private final DoubleProperty rotationAxisX = new SimpleDoubleProperty(this, "rotationAxisX", 1.0);
    private final DoubleProperty rotationAxisY = new SimpleDoubleProperty(this, "rotationAxisY", 0.0);
    private final DoubleProperty rotationAxisZ = new SimpleDoubleProperty(this, "rotationAxisZ", 0.0);
    private final DoubleProperty rotationAngle = new SimpleDoubleProperty(this, "rotationAngle", 0.0);
    private final DoubleProperty distanceFromOrigin = new SimpleDoubleProperty(this, "distanceFromOrigin", 10);
    
    private final ObjectProperty<PerspectiveCamera> camera =
            new SimpleObjectProperty<PerspectiveCamera>(this, "camera", new PerspectiveCamera(true));
    private final NoGarbageProperty<Transform> transformRotationOnly =
            new NoGarbageProperty<Transform>(this, "transformRotationOnly", new Affine());
    private final NoGarbageProperty<Transform> transformRotationTranslation =
            new NoGarbageProperty<Transform>(this, "transformRotationTranslation", new Affine());
    private final NoGarbageProperty<CameraToRasterTransform> transformSceneToRaster =
            new NoGarbageProperty<CameraToRasterTransform>(this, "transformSceneToRaster",
                    new PerspectiveSceneToRaster());
    
    private final InteractionDragArcball arcball = new InteractionDragArcball(rotationAngle,
            rotationAxisX, rotationAxisY, rotationAxisZ);
    private final InteractionScrollZoom scrollZoom = new InteractionScrollZoom(distanceFromOrigin);
    private final InteractionDragZoom dragZoom = new InteractionDragZoom(distanceFromOrigin);
    private final InteractionDragPan pan = new InteractionDragPan(originX, originY, originZ, transformRotationOnly,
            distanceFromOrigin, camera);
    
    private Scene scene = null;
    private SubScene subscene = null;
    
    private void updateTransformRotationOnly() {
        final Affine xform = (Affine)transformRotationOnly.get();
        xform.setToIdentity();
        xform.appendRotation(rotationAngle.get(), 0, 0, 0, 
                             rotationAxisX.get(), rotationAxisY.get(), rotationAxisZ.get());
        transformRotationOnly.fireChangedEvent();
    }
    
    private void updateTransformRotationTranslation() {
        final Affine xform = (Affine)transformRotationTranslation.get();
        xform.setToIdentity();
        xform.appendTranslation(originX.get(), originY.get(), originZ.get());
        xform.append(transformRotationOnly.get());
        xform.appendTranslation(0, 0, -distanceFromOrigin.get());
        transformRotationTranslation.fireChangedEvent();
    }
    
    private void updateTransformSceneToRaster() {
        PerspectiveSceneToRaster s2r = (PerspectiveSceneToRaster)transformSceneToRaster.get();
        if (scene != null) {
            s2r.setParameters(camera.get(), transformRotationTranslation.get(), scene);
        } else if (subscene != null) {
            s2r.setParameters(camera.get(), transformRotationTranslation.get(), subscene);
        }
        transformSceneToRaster.fireChangedEvent();
    }
    
    private void updateTransforms() {
        updateTransformRotationOnly();
        updateTransformRotationTranslation();
        updateTransformSceneToRaster();
        camera.get().getTransforms().setAll(transformRotationTranslation.get());
    }
    
    private void buildListeners() {
        final ChangeListener<Number> triggerUpdateTransformsFromNumber = new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> ob, Number oldValue, Number newValue) {
                updateTransforms();
            }
        };
        originX.addListener(triggerUpdateTransformsFromNumber);
        originY.addListener(triggerUpdateTransformsFromNumber);
        originZ.addListener(triggerUpdateTransformsFromNumber);
        rotationAxisX.addListener(triggerUpdateTransformsFromNumber);
        rotationAxisY.addListener(triggerUpdateTransformsFromNumber);
        rotationAxisZ.addListener(triggerUpdateTransformsFromNumber);
        rotationAngle.addListener(triggerUpdateTransformsFromNumber);
        distanceFromOrigin.addListener(triggerUpdateTransformsFromNumber);
    }
    
}
