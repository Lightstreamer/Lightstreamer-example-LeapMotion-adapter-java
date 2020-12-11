/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.lightstreamer.adapters.LeapMotionDemo.engine3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.croftsoft.core.lang.EnumUnknownException;
import com.croftsoft.core.math.MathLib;
import com.croftsoft.core.math.axis.AxisAngle;
import com.croftsoft.core.math.axis.AxisAngleImp;
import com.croftsoft.core.math.axis.AxisAngleMut;
import com.lightstreamer.adapters.LeapMotionDemo.Constants;

public class BaseModelBody implements IBody {
    
    private Logger logger = LogManager.getLogger(Constants.WORLD_CAT);

    private static final double ROTATE_DELTA    = 0.5;
    private static final double TRANSLATE_DELTA = 0.002;
    private static final double WORLD_SIZE_X = 160;
    private static final double WORLD_SIZE_Y = 90;
    private static final double WORLD_SIZE_Z = 120;
    
    private String id;

    private long    lifeSpan = 0;
    private long    lastCmdRcvd = 0;
    
    private double  x, y, z;                                // position         Vector3
    
    private double  vX, vY, vZ;                             // velocity         Vector3
    private final AxisAngleMut  axisAngle;                  // Spin             Quaternion/Matrix3x3
    private double  deltaRotX, deltaRotY, deltaRotZ;        // angularMomentum  Vector3

    public BaseModelBody(String id) {
        this(id,new AxisAngleImp(),
                (double)((Math.random() * 50) - 25),(double)((Math.random() * 50) - 25),(double)((Math.random() * 50) - 25));
    }
    
    public BaseModelBody(BaseModelBody orig) {
        this(orig.getId(),orig.getAxisAngle(),
                orig.getX(),orig.getY(),orig.getZ(),
                orig.getvX(),orig.getvY(),orig.getvZ(),
                orig.getDeltaRotX(),orig.getDeltaRotY(),orig.getDeltaRotZ());
    }
    
    public BaseModelBody(String id, AxisAngle axisAngle, double x, double y, double z) {
        this(id,axisAngle,x,y,z,0,0,0,0,0,0);
    }
    
    public BaseModelBody(String id, AxisAngle axisAngle, 
            double x, double y, double z, 
            double vX, double vY, double vZ, 
            double deltaRotX, double deltaRotY, double deltaRotZ) {
        
        this.id = id;
        
        this.axisAngle = new AxisAngleImp(axisAngle);
        
        this.x = x;    
        this.y = y;
        this.z = z;
        
        this.vX = vX;
        this.vY = vY;
        this.vZ = vZ;
        
        this.deltaRotX = deltaRotX;
        this.deltaRotY = deltaRotY;
        this.deltaRotZ = deltaRotZ;
        
    }
    
    // --> simple getters/setters
    
    public String getId() {
        return this.id;
    }
    
    public long getLifeSpan() {
        return lifeSpan;
    }
    
    @Override
    public AxisAngle getAxisAngle() {
        return axisAngle;
    }
    
    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public void setAxisAngle(AxisAngle axisAngle) {
        this.axisAngle.copy( axisAngle );
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public void setZ(double z) {
        this.z = z;
    }

    public double getvX() {
        return vX;
    }

    public double getvY() {
        return vY;
    }

    public double getvZ() {
        return vZ;
    }

    public double getDeltaRotX() {
        return deltaRotX;
    }

    public double getDeltaRotY() {
        return deltaRotY;
    }

    public double getDeltaRotZ() {
        return deltaRotZ;
    }
    
    public long getInactivityPeriod() {
        return (this.lifeSpan - this.lastCmdRcvd);
    }
    
    // --> model transformations
    
    @Override
    public void rotate(AxisAngle axisAngleRot) {
        AxisAngleMut newAxisAngleMut = this.axisAngle.toQuat( ).multiply(axisAngleRot.toQuat()).toAxisAngle ( );

        newAxisAngleMut.normalize ();

        this.axisAngle.copy(newAxisAngleMut);
    }

    @Override
    public void rotate(Axis axis, double degrees) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rotate(Rotation rotation, double degrees) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rotate() {
        rotate ( new AxisAngleImp ( this.deltaRotX, 1, 0, 0 ) );
        rotate ( new AxisAngleImp ( this.deltaRotY, 0, 1, 0 ) );
        rotate ( new AxisAngleImp ( this.deltaRotZ, 0, 0, 1 ) );
    }
    
    @Override
    public void rotate(double factor) {
        rotate ( new AxisAngleImp ( this.deltaRotX * factor, 1, 0, 0 ) );
        rotate ( new AxisAngleImp ( this.deltaRotY * factor, 0, 1, 0 ) );
        rotate ( new AxisAngleImp ( this.deltaRotZ * factor, 0, 0, 1 ) );
    }
    
    @Override
    public void translate(Axis axis, double distance) {
        switch ( axis )
        {
            case X:
                this.x += distance;
              
                break;
              
            case Y:
                this.y += distance;
                
                break;
                
            case Z:
                this.z += distance;
                
                break;
            
            default:
                // Skip.
        }
    }
    
    @Override
    public void translate() {
        this.x += (double)(this.vX * TRANSLATE_DELTA);
        this.y += (double)(this.vY * TRANSLATE_DELTA);
        this.z += (double)(this.vZ * TRANSLATE_DELTA);
                
        this.x = MathLib.wrap(this.x, (-0.5 * WORLD_SIZE_X), WORLD_SIZE_X);
        this.y = MathLib.wrap(this.y, (-0.5 * WORLD_SIZE_Y), WORLD_SIZE_Y);
        this.z = MathLib.wrap(this.z, (-0.5 * WORLD_SIZE_Z), WORLD_SIZE_Z);
        
        this.lifeSpan += 1;
    }
    
    @Override
    public void translate(double factor) {
        this.x += (double)(this.vX * TRANSLATE_DELTA * factor);
        this.y += (double)(this.vY * TRANSLATE_DELTA * factor);
        this.z += (double)(this.vZ * TRANSLATE_DELTA * factor);
                
        this.x = MathLib.wrap(this.x, (-0.5 * WORLD_SIZE_X), WORLD_SIZE_X);
        this.y = MathLib.wrap(this.y, (-0.5 * WORLD_SIZE_Y), WORLD_SIZE_Y);
        this.z = MathLib.wrap(this.z, (-0.5 * WORLD_SIZE_Z), WORLD_SIZE_Z);
        
        this.lifeSpan += 1;
    }

    @Override
    public void translate(Translation translation, double distance) {
        // TODO Auto-generated method stub

    }
    
    //user inputs -->
    
    public void setImpulse(Axis axis, double intensity) {
        switch ( axis )
        {
            case X:
                this.vX += intensity;
                
                break;
                
            case Y:
                this.vY += intensity;
                
                break;
                
            case Z:
                this.vZ += intensity;
                
                break;
                
            default:
                throw new EnumUnknownException ( axis );
        }
        this.lastCmdRcvd = this.lifeSpan;
    }
    
    public void setTourque(Axis axis, double intensity) {
        switch ( axis )
        {
            case X:
                this.deltaRotX += (intensity * ROTATE_DELTA);
                
                break;
                
            case Y:
                this.deltaRotY += (intensity  * ROTATE_DELTA);
                
                break;
                
            case Z:
                this.deltaRotZ += (intensity  * ROTATE_DELTA);
                
                break;
                
            default:
                throw new EnumUnknownException ( axis );
        }
        this.lastCmdRcvd = this.lifeSpan;
    }
    
    public void block() {
        this.vX = 0;
        this.vY = 0;
        this.vZ = 0;
        
        this.deltaRotX = 0;
        this.deltaRotY = 0;
        this.deltaRotZ = 0;
    }
    
    public void forcePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // data extraction -->
    
    private static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }
    
    private static String toBase64(double value) throws IOException {
        String s = (new Base64Manager()).encodeBytes(toByteArray((float)value),true);
        return s.substring(0, s.indexOf("="));
    }
    
    public void fillPositionMap(HashMap<String,String> model) {
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|preparing position update");
        }
        
        try {
            model.put("posX", toBase64(this.x));
            model.put("posY", toBase64(this.y));
            model.put("posZ", toBase64(this.z));
            
            model.put("rotX", toBase64(this.axisAngle.toQuat().getX()));
            model.put("rotY", toBase64(this.axisAngle.toQuat().getY()));
            model.put("rotZ", toBase64(this.axisAngle.toQuat().getZ()));
            model.put("rotW", toBase64(this.axisAngle.toQuat().getW()));
            
        } catch (IOException e) {
            //TODO ?
        }
    }
    
    public void fillImpulseMap(HashMap<String,String> model) {
        if (logger.isTraceEnabled()) {
            logger.trace(this.id+"|preparing speed update");
        }
        
        model.put("dVx", String.valueOf(this.vX));
        model.put("dVy", String.valueOf(this.vY));
        model.put("dVz", String.valueOf(this.vZ));
        
        model.put("dRx", String.valueOf(this.deltaRotX));
        model.put("dRy", String.valueOf(this.deltaRotY));
        model.put("dRz", String.valueOf(this.deltaRotZ));
    }
    
  

}
