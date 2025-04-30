package com.xus.mongo2sql.llm;
public class MongoCommand {
		private String sourceCommand;
		private String command;
		private String collectionName; // 添加collectionName属性
		private int commandSize;
		private int index;

		public MongoCommand(String sourceCommand,String command, String collectionName, int commandSize, int index) {
			this.sourceCommand =sourceCommand;
			this.command = command;
			this.collectionName = collectionName;
			this.commandSize = commandSize;
			this.index = index;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		// 添加collectionName的getter和setter方法
		public String getCollectionName() {
			return collectionName;
		}

		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}

		public int getCommandSize() {
			return commandSize;
		}

		public void setCommandSize(int commandSize) {
			this.commandSize = commandSize;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String getSourceCommand() {
			return sourceCommand;
		}

		public void setSourceCommand(String sourceCommand) {
			this.sourceCommand = sourceCommand;
		}
	}