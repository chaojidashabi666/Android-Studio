package com.qrs.maincarcontrolapp.msg;

import com.qrs.maincarcontrolapp.constants.Commands;

import java.nio.ByteBuffer;

public class TrafficLightRes {

    byte id=0x1B;

    byte result;

    private byte lightColor = 0;

    static byte len = 0x07;

    static byte bodyLen = 0x01;



    public byte[] pack(){

        ByteBuffer buffer = ByteBuffer.allocate(len);
        //主车标识
        buffer.put(Commands.FRAME_HEAD_0);
        //Android标识
        buffer.put(Commands.FRAME_HEAD_1);
        //主指令
        if (lightColor != 0) {
            buffer.put(Commands.TRAFFIC_LIGHT_SUCCESS);
        } else {
            buffer.put(Commands.TRAFFIC_LIGHT_FAILED);
        }
        //数据长度
        buffer.put(bodyLen);
        //交通灯识别结果
        buffer.put(lightColor);
        //校验
        byte crc = (byte) ((bodyLen + lightColor) % 0xFF);
        buffer.put(crc);
        //帧尾
        buffer.put(Commands.FRAME_END);
        //buffer.put(new String("abc").getBytes());


        return buffer.array();

    }

    public void setLightColor(byte lightColor) {
        this.lightColor = lightColor;
    }


}
