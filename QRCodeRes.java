package com.qrs.maincarcontrolapp.msg;

import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.tools.LogUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class QRCodeRes {

    private byte[] qrCode = null;

    static byte len = 30;

    static byte bodyLen = 1;



    public byte[] pack(){

        ByteBuffer buffer = ByteBuffer.allocate(len);
        //主车标识
        buffer.put(Commands.FRAME_HEAD_0);
        //Android标识
        buffer.put(Commands.FRAME_HEAD_1);
        //主指令
        if (qrCode != null && qrCode.length != 0) {
            buffer.put(Commands.QR_SUCCESS_1);
            //数据长度
            buffer.put(bodyLen);
            //文本识别结果
            buffer.put(qrCode);
        } else {
            LogUtil.log("没有识别到二维码！");
            buffer.put(Commands.QR_FAILED);
            //数据长度
            buffer.put(bodyLen);
            //识别结果
            buffer.put((byte) 0);
        }
        //校验
        byte crc = getCRCNum(qrCode);
        buffer.put(crc);
        //帧尾
        buffer.put(Commands.FRAME_END);


        return subArray(buffer.array(), Commands.FRAME_END);

    }

    public void setQrCode(byte[] qrCode) {
        if (qrCode != null && qrCode.length != 0) {
            this.qrCode = new byte[qrCode.length];
            for (int i = 0; i < qrCode.length; i++) {
                this.qrCode[i] = qrCode[i];
            }
            this.bodyLen = (byte) qrCode.length;
        } else {
            this.bodyLen = 1;
        }
    }

    public byte getCRCNum(byte[] bytes) {
        if (bytes != null && bytes.length != 0) {
            byte sum = bodyLen;
            for (byte b : bytes) {
                sum += b;
            }
            return (byte) (sum % 0XFF);
        } else {
            return (byte) (bodyLen % 0XFF);
        }

    }

    public byte[] subArray(byte[] bytes, byte element) {
        int index = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == element) {
                index = i + 1;
            }
        }

        return Arrays.copyOfRange(bytes,0,index);
    }

}
