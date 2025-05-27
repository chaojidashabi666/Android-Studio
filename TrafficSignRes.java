package com.qrs.maincarcontrolapp.msg;

import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.tools.LogUtil;

import java.nio.ByteBuffer;

public class TrafficSignRes {

        private byte sign = 0;

        //总长度
        static byte len = 0x07;

        //回传数据长度
        static byte bodyLen = 0x01;



        public byte[] pack(){

            ByteBuffer buffer = ByteBuffer.allocate(len);
            //主车标识
            buffer.put(Commands.FRAME_HEAD_0);
            //Android标识
            buffer.put(Commands.FRAME_HEAD_1);
            //主指令
            if (sign != 0) {
                buffer.put(Commands.TRAFFIC_SIGN_SUCCESS);
            } else {
                LogUtil.log("交通标志识别失败！");
                buffer.put(Commands.TRAFFIC_SIGN_FAILED);
            }
            //数据长度
            buffer.put(bodyLen);
            //交通灯识别结果
            buffer.put(sign);
            //校验
            byte crc = (byte) ((bodyLen + sign) % 0xFF);
            buffer.put(crc);
            //帧尾
            buffer.put(Commands.FRAME_END);
            //buffer.put(new String("abc").getBytes());


            return buffer.array();

        }

        public void setTrafficSign(byte lightColor) {
            this.sign = lightColor;
        }




}
