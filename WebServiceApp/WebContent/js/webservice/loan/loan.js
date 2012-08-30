/**
 * 请款
 */
function cashAdvance(){
	/**
	 * 渠道信息数据解析器
	 */
	var cashReader = new Ext.data.JsonReader({
		totalProperty : "totalCount",
		root : "cashList"
	},[
	   {name:"id"},
	   {name:"cardId"},
	   {name:"cashAmount"},
	   {name:"cashDate"},
	   {name:"cashReason"},
	   {name:"cashRemark"},
	   {name:"cashUserId"},
	   {name:"cashUserName"},
	   {name:"cashCheckUserId"},
	   {name:"cashCheckUserName"},
	   {name:"cashCheckDate"},
	   {name:"cashCheckResult"},
	   {name:"cashApprovalUserId"},
	   {name:"cashApprovalUserName"},
	   {name:"cashApprovalDate"},
	   {name:"cashApprovalResult"},
	   {name:"processTaskId"},
	   {name:"cashStatus"}
	]);
	/**
	 * 渠道信息数据集
	 */
	var cashDataStore = new Ext.data.Store({
		proxy:new Ext.data.HttpProxy({
			url: path + "/loan/myRequestCashAdvance.action?method=myRequest"
		}),
		reader:cashReader,
		//倒叙排序
		sortInfo:{field: 'cashDate', direction: 'DESC'},
		listeners:{
			"loadexception":function(loader, node, response){
				try{
					if(response.status == "200"){
						try{
							var re = Ext.decode(response.responseText);
							if(re){
								Ext.Msg.alert('错误提示',re.msg, function(btn){
								});
							}
						}catch(e){
							Ext.Msg.alert('错误提示',"系统错误！错误代码："+e, function(btn){
							});
						}
					}else{
						httpStatusCodeHandler(response.status);
					}
				}catch(e){
					Ext.Msg.alert('错误提示',"系统错误！错误代码："+e, function(btn){
					});
				}
			}
		}
	});
	//数据展现样式
	var cashSM = new Ext.grid.CheckboxSelectionModel();
	//展示样式
	var cashCM = new Ext.grid.ColumnModel([new Ext.grid.RowNumberer(),cashSM,{
		dataIndex:"id",
		hidden:true,
		hideable:false
	},{
		header:"卡号",
		dataIndex:"cardId",
		hidden:true,
		hideable:false,
		width:100
	},{
		header:"请款金额",
		dataIndex:"cashAmount",
		width:70,
	},{
		header:"请款日期",
		dataIndex:"cashDate",
		renderer:new Ext.util.Format.dateRenderer("Y-m-d"),
		sortable:true,
		width:80
	},{
		header:"请款原因",
		dataIndex:"cashReason",
		width:100
	},{
		header:"请款人",
		dataIndex:"cashUserName",
		width:70
	},{
		header:"审核人",
		dataIndex:"cashCheckUserName",
		width:70
	},{
		header:"审批人",
		dataIndex:"cashApprovalUserName",
		width:70
	},{
		header:"请款审批状态",
		dataIndex:"cashStatus",
		renderer:cashStatusRender,
		sortable:true,
		width:70
	}]);
	//查询参数
	var params = {};
	/**
	 * 查询表单
	 */
	var searchPanel = new Ext.form.FormPanel({
		id:"searchPanel",
		buttonAlign:"right",
		labelAlign:"right",
		border:true,
		frame:true,
		labelWidth:80,
		//autoHeight:true,
		height:80,
		layout:'column',
		region:'north',
		items:[{
			columnWidth:.20,
			layout:'form',
			items:[getDateField("cashAdvanceInfo.cashDate", "请款日期", true, false)]
		},{
			columnWidth:.20,
			layout:'form',
			items:[getComboBoxField("cashAdvanceInfo.cashStatus", statusStore ,"请款审批状态", "dataKey", "dataValue", true, false)]
		}],
		buttons:[{
			text:"查询",
			handler:function(){
				var baseParams = searchPanel.getForm().getValues();
				baseParams.start = 0;
				baseParams.limit = 50;
				baseParams["cashAdvanceInfo.cashUserName"] = userName;
				baseParams["cashAdvanceInfo.cashUserId"] = userId;
				cashDataStore.baseParams = baseParams;
				loadCashDataStore();
			}
		},{
			text:"重置",
			handler:function(){
				searchPanel.form.reset();
				var baseParams = {};
				baseParams.start = 0;
				baseParams.limit = 50;
				baseParams["cashAdvanceInfo.cashUserName"] = userName;
				baseParams["cashAdvanceInfo.cashUserId"] = userId;
				cashDataStore.baseParams = baseParams;
			}
		}]
	});
	/**
	 * 列表
	 */
	var cashGrid = new Ext.grid.GridPanel({
		id:"cashGrid",
		title:"请款管理",
		region:'center',
		collapsible:false,//是否可以展开
		animCollapse:true,//展开时是否有动画效果
		autoScroll:true,
		//width:Ext.get("channel_main_div").getWidth(),
		//height:Ext.get("channel_main_div").getHeight(),
		loadMask:true,//载入遮罩动画（默认）
		view: new Ext.grid.GridView({ forceFit:true }),
		plugins: [new Ext.ux.ColumnWidthCalculator()],
		frame:true,
		autoShow:true,		
		store:cashDataStore,
		cm:cashCM,
		sm:cashSM,
		//renderTo:"channel_main_div",
		viewConfig:{forceFit:true},//若父容器的layout为fit，那么强制本grid充满该父容器
		split: true,
		stripeRows: true,
		bbar:new Ext.PagingToolbar({
			pageSize:50,//每页显示数
			store:cashDataStore,
			displayInfo:true,
			displayMsg:"显示{0}-{1}条记录，共{2}条记录",
			nextText:"下一页",
			prevText:"上一页",
			emptyMsg:"无相关记录"
		})
	})
	/**
	 * 主面板
	 */
	var mainPanel = new Ext.Panel({
		layout:'border',
		height:Ext.get("loan_div").getHeight() - 15,
		renderTo:"loan_div",
		items:[searchPanel, cashGrid],
		tbar:[]
	})
	
	/**
	 * 加载数据
	 */
	function loadCashDataStore(){
		cashDataStore.load({
			params:params
		});
	}
	/**
	 * 加载数据参数
	 */
	function loadStoreParams(){
		params.start = 0;
		params.limit = 50;
		params["cashAdvanceInfo.cashUserName"] = userName;
		params["cashAdvanceInfo.cashUserId"] = userId;
		cashDataStore.baseParams = params;
	}
	loadStoreParams();
	/**
	 * 按钮存储器，尚未执行查询
	 */
	var buttonRightStore = buttonRight();
	/**
	 * 执行权限按钮加载, 并且加载列表数据, 显示权限按钮
	 * see buttonRight.js
	 * loadButtonRight(buttonStore, mainDataStore, dataGrid, pageDiv, params)
	 */
	loadButtonRight(buttonRightStore, cashDataStore, mainPanel, "loan_div");
	/**
	 * 加载请款下拉框
	 */
	statusStore.load({params:{codeId:"297e27f139755679013975a50522007d",codeName:"请款状态"}});
	/**
	 * 解析列表上的当前状态
	 */
	function cashStatusRender(value){
		if(value && value != ""){
			return getCodeNameFromStore(value, statusStore, "dataKey", "dataValue");
		}
		return value;
	}
	/**
	 * 新增请款
	 */
	this.addLoanRequest = function(url){
		var reqForm = getLoanRequestForm(url, false, false);
		var buttons = [{
			text:"暂时保存",
			handler:function(){
				if(reqForm.form.isValid()){
					//00-申请请款
					reqForm.form.findField("cashAdvanceInfo.cashStatus").setValue("00");
					saveLoadRequest("addLoanRequestWindow", reqForm);
				}
			}
		},{
			text:"直接提交",
			handler:function(){
				if(reqForm.form.isValid()){
					//01-发起审核
					reqForm.form.findField("cashAdvanceInfo.cashStatus").setValue("01");
					saveLoadRequest("addLoanRequestWindow", reqForm);
				}
			}
		},{
			text:"取消请款",
			handler:function(){
				var w = Ext.getCmp("addLoanRequestWindow");
				if(w){
					w.close();
				}
			}
		}];
		showAllWindow("addLoanRequestWindow", "新增请款", 500, 300, reqForm, null, buttons);
	};
	/**
	 * 新增/修改/详细表单
	 * @param url 表单提交地址
	 * @param isNull 是否允许空
	 * @param readOnly 只读
	 */
	function getLoanRequestForm(url,isNull, readOnly){
		var requestForm = new Ext.form.FormPanel({
			labelAlign:"right",
			border:true,
			frame:true,
			url:url,
			labelWidth:80,
			items:[{
				layout:'column',
				height:40,
				items:[{
					columnWidth:.50,
					layout:'form',
					items:[getTextField("cashAdvanceInfo.cardId", "入账卡号", isNull, readOnly), getHiddenField("cashAdvanceInfo.id", "")]
				},{
					columnWidth:.50,
					layout:'form',
					items:[getNumberField("cashAdvanceInfo.cashAmount", "请款金额(元)", isNull, readOnly), getHiddenField("cashAdvanceInfo.cashUserName", userName)]
				}]
			},{
				layout:'column',
				height:40,
				items:[{
					columnWidth:.50,
					layout:'form',
					items:[getDateField("cashAdvanceInfo.cashDate", "请款日期", isNull, readOnly, new Date()), getHiddenField("cashAdvanceInfo.cashUserId", userId)]
				}]
			},{
				layout:'column',
				height:70,
				items:[{
					columnWidth:.90,
					layout:'form',
					items:[getTextAreaField("cashAdvanceInfo.cashReason", "请款原因", isNull, readOnly)]
				}]
			},{
				layout:'column',
				height:70,
				items:[{
					columnWidth:.90,
					layout:'form',
					items:[getTextAreaField("cashAdvanceInfo.cashRemark", "备注", true, readOnly)]
				}]
			},
			getHiddenField("cashAdvanceInfo.cashCheckUserId"), getHiddenField("cashAdvanceInfo.cashCheckUserName"),
			getHiddenField("cashAdvanceInfo.cashCheckDate"), getHiddenField("cashAdvanceInfo.cashCheckResult"),
			getHiddenField("cashAdvanceInfo.cashApprovalUserId"), getHiddenField("cashAdvanceInfo.cashApprovalUserName"),
			getHiddenField("cashAdvanceInfo.cashApprovalDate"), getHiddenField("cashAdvanceInfo.cashApprovalResult"),
			getHiddenField("cashAdvanceInfo.processTaskId"), getHiddenField("cashAdvanceInfo.cashStatus")
			]
		})
		return requestForm;
	}
	/**
	 * 保存请款
	 * @param windowId
	 * @param form
	 */
	function saveLoadRequest(windowId, form){
		Ext.MessageBox.show({
			msg:"正在保存请款信息，请稍候...",
			progressText:"正在保存请款信息，请稍候...",
			width:300,
			wait:true,
			waitConfig: {interval:200},
			icon:Ext.Msg.INFO
		});
		form.getForm().submit({
			success: function(form, action) {
				Ext.Msg.hide();
				try{
					var result = Ext.decode(action.response.responseText);
					if(result && result.success){
						var msg = "请款信息保存成功！";
						if(result.msg){
							msg = result.msg;
						}
						Ext.Msg.alert('系统提示信息', msg, function(btn, text) {
							if (btn == 'ok') {
								Ext.getCmp(windowId).close();
								loadCashDataStore();
							}
						});
					}else if(!result.success){
						var msg = "系统消息发送失败！";
						if(result.msg){
							msg = result.msg;
						}
						Ext.Msg.alert('系统提示信息', msg);
					}
				}catch(e){
					Ext.Msg.alert('系统提示信息', "系统错误：" + e);
				}
			},
			failure: function(form, action) {//action.result.errorMessage
				Ext.Msg.hide();
				var msg = "系统消息发送失败，请检查您的网络连接或者联系管理员！";
				try{
					var result = Ext.decode(action.response.responseText);
					if(result.msg){
						msg = result.msg;
					}
				}catch(e){
					msg = "系统错误：" + e;
				}
				Ext.Msg.alert('系统提示信息', msg);
			}
		});
	}
}
/**
 * 程序主入口
 */
Ext.onReady(function(){
	Ext.QuickTips.init();
	Ext.form.Field.prototype.msgTarget = 'side';
	cashAdvance();
});