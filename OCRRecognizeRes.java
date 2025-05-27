package com.qrs.maincarcontrolapp.msg;

import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.tools.LogUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class OCRRecognizeRes {

    private byte[] OCRCarID = null;

    private byte[] OCRText = null;

    static byte len = 30;

    static byte carIDLen = 12;

    static byte bodyLen = 1;



    public byte[] packText(){

        ByteBuffer buffer = ByteBuffer.allocate(len);
        //主车标识
        buffer.put(Commands.FRAME_HEAD_0);
        //Android标识
        buffer.put(Commands.FRAME_HEAD_1);
        //主指令
        if (OCRText != null && OCRText.length != 0) {
            buffer.put(Commands.OCR_TEXT_SUCCESS);
            //数据长度
            buffer.put(bodyLen);
            //文本识别结果
            buffer.put(OCRText);
        } else {
            LogUtil.log("OCR识别失败！");
            buffer.put(Commands.OCR_TEXT_FAILED);
            //数据长度
            buffer.put(bodyLen);
            //识别结果
            buffer.put((byte) 0);
        }
        //校验
        byte crc = getCRCNum(OCRText);
        buffer.put(crc);
        //帧尾
        buffer.put(Commands.FRAME_END);


        return subArray(buffer.array(), Commands.FRAME_END);

    }

    public byte[] packCarID(){

        ByteBuffer buffer = ByteBuffer.allocate(carIDLen);
        //主车标识
        buffer.put(Commands.FRAME_HEAD_0);
        //Android标识
        buffer.put(Commands.FRAME_HEAD_1);
        //主指令
        if (OCRCarID != null && OCRCarID.length != 0) {
            buffer.put(Commands.CAR_ID_SUCCESS_FIRST);
            //数据长度
            buffer.put(bodyLen);
            //OCR文本识别结果
            buffer.put(OCRCarID);
        } else {
            LogUtil.log("车牌识别失败！");
            buffer.put(Commands.CAR_ID_FAILED);
            //数据长度
            buffer.put(bodyLen);
            //OCR文本识别结果
            buffer.put((byte) 0);
        }

        //校验
        byte crc = getCRCNum(OCRCarID);
        buffer.put(crc);
        //帧尾
        buffer.put(Commands.FRAME_END);

        return subArray(buffer.array(), Commands.FRAME_END);
    }

    public byte[] packBrokenCarID(){

        ByteBuffer buffer = ByteBuffer.allocate(carIDLen);
        //主车标识
        buffer.put(Commands.FRAME_HEAD_0);
        //Android标识
        buffer.put(Commands.FRAME_HEAD_1);
        //主指令
        if (OCRCarID != null && OCRCarID.length != 0) {
            buffer.put(Commands.BROKEN_CAR_ID_SUCCESS_FIRST);
            //数据长度
            buffer.put(bodyLen);
            //OCR文本识别结果
            buffer.put(OCRCarID);
        } else {
            LogUtil.log("破损车牌识别失败！");
            buffer.put(Commands.BROKEN_CAR_ID_FAILED);
            //数据长度
            buffer.put(bodyLen);
            //OCR文本识别结果
            buffer.put((byte) 0);
        }

        //校验
        byte crc = getCRCNum(OCRCarID);
        buffer.put(crc);
        //帧尾
        buffer.put(Commands.FRAME_END);

        return subArray(buffer.array(), Commands.FRAME_END);
    }

    public void setOCRText(byte[] OCRText) {
        if (OCRText != null && OCRText.length != 0) {
            this.OCRText = new byte[OCRText.length];
            for (int i = 0; i < OCRText.length; i++) {
                this.OCRText[i] = OCRText[i];
            }
            this.bodyLen = (byte) OCRText.length;
        } else {
            this.bodyLen = 1;
        }
    }

    public void setOCRCarID(byte[] OCRCarID) {
        //this.OCRCarID = OCRCarID;
        if (OCRCarID != null && OCRCarID.length != 0) {
            this.OCRCarID = new byte[OCRCarID.length];
            for ( int i = 0; i < OCRCarID.length; i++) {
                this.OCRCarID[i] = OCRCarID[i];
            }
            this.bodyLen = (byte) OCRCarID.length;
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
