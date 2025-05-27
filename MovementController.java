/*
 * Copyright (c) 2022. UnknownNetworkService Group
 * This file is created by UnknownObject at 2022 - 11 - 8
 */

package com.qrs.maincarcontrolapp.gui;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.qrs.maincarcontrolapp.communicate.DataTransferCore;
import com.qrs.maincarcontrolapp.R;
import com.qrs.maincarcontrolapp.communicate.DataTransferCore;
import com.qrs.maincarcontrolapp.communicate.SerialPortTransferCore;
import com.qrs.maincarcontrolapp.communicate.WifiTransferCore;
import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.constants.Flags;

//主车和从车的移动控制
//实现简单，不太稳定，仅供娱乐和调试用途
public class MovementController extends AppCompatActivity
{
	
	//存储小车的IP
	private String IPCar;
	//通信客户端
	private DataTransferCore dtc_client;
	//DHCP相关信息，用于获取IP地址
	private DhcpInfo dhcpInfo;
	//WIFI管理器
	private WifiManager wifiManager;
	//通信通道标志，true为Wifi，false为串口
	private final boolean CommunicationUsingWifi = true;
	//串口通信的设备名
	private final String SerialPortPath = "/dev/ttyS4";
	//移动指令
	private final byte[] cmd_data = new byte[8];
	//编辑框——距离
	EditText et_distance;
	//编辑框——转向速度
	EditText et_turn_speed;
	//编辑框——循迹速度
	EditText et_run_peed;
	
	private int GetDistance()
	{
		String str = et_distance.getText().toString();
		try
		{
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	private int GetTurnSpeed()
	{
		String str = et_turn_speed.getText().toString();
		try
		{
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	private int GetRunSpeed()
	{
		String str = et_run_peed.getText().toString();
		try
		{
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_movement_controller);
		
		//设置标题栏
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setTitle("主/从车远端移动控制");
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		
		//获取控件对象
		et_distance = findViewById(R.id.num_distance);
		et_run_peed = findViewById(R.id.num_run_speed);
		et_turn_speed = findViewById(R.id.num_turn_speed);
		
		//获取主车IP地址
		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		dhcpInfo = wifiManager.getDhcpInfo();
		IPCar = Formatter.formatIpAddress(dhcpInfo.gateway);
		
		//建立连接
		if (CommunicationUsingWifi)
			dtc_client = null;
			//dtc_client = new WifiTransferCore(IPCar, 60000, new Handler());
		else
			dtc_client = new SerialPortTransferCore(SerialPortPath, 115200, new Handler());
		(new Thread(() -> dtc_client.Connect())).start();
//		dtc_client.EnableAutoReconnect();	//启动自动重连
		
		cmd_data[0] = Commands.FRAME_HEAD_0;
		cmd_data[1] = Flags.CMD_PACKET_MAIN_CAR;
		cmd_data[7] = Commands.FRAME_END;
		
		findViewById(R.id.rb_main_car).setOnClickListener(view -> cmd_data[1] = Flags.CMD_PACKET_MAIN_CAR);
		
		findViewById(R.id.rb_sub_car).setOnClickListener(view -> cmd_data[1] = Flags.CMD_PACKET_SUB_CAR);
		
		findViewById(R.id.btn_front).setOnClickListener(view ->
		{
			cmd_data[2] = Flags.CMD_PACKET_MOVE_FORWARD;
			cmd_data[3] = (byte) (GetRunSpeed() & 0xFF);
			cmd_data[4] = (byte) (GetDistance() & 0xFF);
			cmd_data[5] = (byte) (GetDistance() >> 8);
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
		
		findViewById(R.id.btn_back).setOnClickListener(view -> {
			cmd_data[2] = Flags.CMD_PACKET_MOVE_BACKWARD;
			cmd_data[3] = (byte) (GetRunSpeed() & 0xFF);
			cmd_data[4] = (byte) (GetDistance() & 0xFF);
			cmd_data[5] = (byte) (GetDistance() >> 8);
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
		
		findViewById(R.id.btn_left).setOnClickListener(view -> {
			cmd_data[2] = Flags.CMD_PACKET_MOVE_LEFT;
			cmd_data[3] = (byte) (GetTurnSpeed() & 0xFF);
			cmd_data[4] = (byte) (GetDistance() & 0xFF);
			cmd_data[5] = (byte) (GetDistance() >> 8);
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
		
		findViewById(R.id.btn_right).setOnClickListener(view -> {
			cmd_data[2] = Flags.CMD_PACKET_MOVE_RIGHT;
			cmd_data[3] = (byte) (GetTurnSpeed() & 0xFF);
			cmd_data[4] = (byte) (GetDistance() & 0xFF);
			cmd_data[5] = (byte) (GetDistance() >> 8);
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
		
		findViewById(R.id.btn_stop).setOnClickListener(view -> {
			cmd_data[2] = Flags.CMD_PACKET_MOVE_STOP;
			cmd_data[3] = (byte) 0x00;
			cmd_data[4] = (byte) 0x00;
			cmd_data[5] = (byte) 0x00;
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
		
		findViewById(R.id.btn_run_to_line).setOnClickListener(view -> {
			cmd_data[2] = Flags.CMD_PACKET_MOVE_TO_LINE;
			cmd_data[3] = (byte) (GetRunSpeed() & 0xFF);
			cmd_data[4] = (byte) 0x00;
			cmd_data[5] = (byte) 0x00;
			cmd_data[6] = (byte) ((cmd_data[2] + cmd_data[3] + cmd_data[4] + cmd_data[5]) % 0xFF);
			dtc_client.ThreadSend(cmd_data);
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		//标题栏返回键
		if (item.getItemId() == android.R.id.home)
		{
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		dtc_client.DisableAutoReconnect();
		dtc_client.CloseConnection();
	}
}