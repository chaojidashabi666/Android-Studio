/*
 * Copyright (c) 2023. UnknownNetworkService Group
 * This file is created by UnknownObject at 2023 - 5 - 1
 */

package com.qrs.maincarcontrolapp.gui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.qrs.maincarcontrolapp.R;
import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.constants.Flags;

import java.util.Objects;

public class RaceTasks extends AppCompatActivity
{
	
	@SuppressLint("StaticFieldLeak")
	protected static MainActivity Parent = null;    //用于调用MainActivity内函数的静态成员变量
	
	//初始化比赛任务按钮
	private void InitTaskButton()
	{
		findViewById(R.id.btn_task_0).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_0));
		findViewById(R.id.btn_task_1).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_1));
		findViewById(R.id.btn_task_2).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_2));
		findViewById(R.id.btn_task_3).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_3));
		findViewById(R.id.btn_task_4).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_4));
		findViewById(R.id.btn_task_5).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_5));
		findViewById(R.id.btn_task_6).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_6));
		findViewById(R.id.btn_task_7).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_7));
		findViewById(R.id.btn_task_8).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_8));
		findViewById(R.id.btn_task_9).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_9));
		findViewById(R.id.btn_task_10).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_10));
		findViewById(R.id.btn_task_11).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_11));
		findViewById(R.id.btn_task_12).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_12));
		/*findViewById(R.id.btn_task_13).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_13));
		findViewById(R.id.btn_task_14).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_14));
		findViewById(R.id.btn_task_15).setOnClickListener(view -> Parent.SendTaskCommand(Commands.TASK_NUMBER_15));*/
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_race_tasks);
		
		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序 - 比赛任务");
		
		if (Parent == null)
		{
			Toast.makeText(this, "Context is null!", Toast.LENGTH_SHORT).show();
			Log.e(Flags.SUB_ACTIVITY_TAG, "Context IS NULL");
			finish();
		}
		
		InitTaskButton();
	}
}