$(document).ready(function(){
	
	
	var columns = [{
	      field: 'id',
	      title: '记录id',
	      align: 'center',
	      visible:false
	  }, {
	      field: 'name',
	      title: '分组名',
	      align: 'center'
	  }, {
	      field: 'owner',
	      title: '负责人',
	      align: 'center'
	  },{
	      field: 'token',
	      title: '密匙',
	      align: 'center',
	      visible:false
	    
	  },{
		  field: 'remark',
	      title: '备注',
	      align: 'center'    
	  },{
	      field: 'createTime',
	      title: '创建时间',
	      align: 'center',
	      formatter:function(value,row,index){
	    	  var date = new Date(value);
              return date.Format("yyyy-MM-dd hh:mm:ss");
	    	  
	      }
	  },{
		  field: 'updateTime',
	      title: '更新时间',
	      align: 'center',
	      formatter:function(value,row,index){
	    	  var date = new Date(value);
              return date.Format("yyyy-MM-dd hh:mm:ss");
	    	  
	      }
		  
	  } ,{
	      title: '操作',
	      field: '',
	      align: 'center',
	      formatter:function(value,row,index){
	    	  //var data = JSON.stringify(row);
	    	 // console.log(row);
	          var e = '<a  class="editBtn"  data-rowId='+row.id+' data-toggle="modal" data-target="" onclick="edit(\''+ row + '\')">编辑</button> ';
	          var d = '<a  id="delBtn" onclick="del(\''+ row.id +'\')">删除</button> ';
	          return e+d;
	      }
	    
	  }
	];
	
	
	initTable("/group/dataList",columns); 
	
	$("#saveBtn").click(saveOrUpdate);
	
    $("#btn_add").click(function(){
       $("#myModel").modal("show");
       $("#id").val("");
       $("#name").val("");
       $("#owner").val("");
       $("#token").val("");
       $("#remark").val("");
       $("#name").removeAttr("disabled");
    });
	
	$("#delBtn").hover(function(){
		$(this).css('cursor','hand');
	});

 });
$(document).on('click','.editBtn', function(){
    var token=prompt("请输入所属组密匙!");
    if(token != null && token != ""){
           	var id = $(this).attr('data-rowId');
            var row = window.rowdata.find(ro => ro.id == id);
              $.ajax({
                           type: "post",
                           data: {
                              groupId:row.id,
                              token:token
                           },
                           url: '/group/checkForToken',
                           success: function (data) {
                               if(data == true){
                                     $("#myModel").modal("show");
                                     $("#id").val(row.id);
                                     $("#name").val(row.name);
                                     $("#owner").val(row.owner);
                                     $("#token").val(row.token);
                                     $("#remark").val(row.remark);
                                     $("#name").attr("disabled","disabled");
                               }else{
                                 alert("输入的组密匙不正确");
                               }
                           },
                           error:function(){

                           }
                       });

    }else if(token == null){
      //点击取消

    }else{
        alert("未输入密匙");
    }
})

function edit(row){
//	var data = JSON.stringify(row)
//	 console.log(row);
//	 console.log(data);
//	 alert(data.id);
//	 alert(row.id);
	
}


function saveOrUpdate(){
	var id = $("#id").val();
	var groupId = (id == null || id== "") ? 0 : id;
	var name = $("#name").val();
	var owner= $("#owner").val();
	var token= $("#token").val();
	var remark = $("#remark").val();
	var url="";
	if(name == null || name == ""){
		alert("请输入数据");
		return;
	}
    if(checkName(name) == null){
        alert("名称只能由数字、字母、中横线和下划线组成");
        return;
    }
    if(token == null || token == ""){
        alert("请输入密匙");
        return;
    }
	if(groupId > 0){//修改
		url="/group/update"
	}else{//新增
		url="/group/add"
	}
	   $.ajax({
           type: "post",
           data: {
        	   groupId:groupId,
        	   name:name,
        	   owner:owner,
        	   token:token,
        	   remark:remark
           },
           url: url,
           success: function (data) {
        	   if(data == true){
        		  alert("操作成功"); 
        		  $("#data_table").bootstrapTable('refresh');
        	   }else{
        		  alert("操作失败"); 
        	   }
             
           }
       });
}



function del(rowId){
         var token=prompt("请输入所属组密匙!");
            if(token != null && token != ""){
                checkToken(rowId,token);
            }else if(token == null){
              //点击取消

            }else{
                alert("未输入密匙");
            }
}

function checkToken(rowId,token){
        $.ajax({
                      type: "post",
                      data: {
                         groupId:rowId,
                         token:token
                      },
                      url: '/group/checkForToken',
                      success: function (data) {
                          if(data == true){
                              preDelete(rowId);
                          }else{
                            alert("输入的组密匙不正确");
                          }
                      },
                      error:function(){

                      }
                  });

}

function preDelete(rowId){

       $.ajax({
                type: "post",
                data: {
                   groupId:rowId
                },
                url: '/app/checkForDelete',
                success: function (data) {
                    if(data == true){
                        alert("存在关联数据，不允许删除");
//                       var opreator=confirm("存在关联数据，是否强制删除(包括应用和开关)");
//                       if(opreator == true){
//                            alert("guanlianshanchu");
//                        }else{
//
//                              alert("quxiao");
//                       }
                    }else{
                        deleteData(rowId);
                    }
                },
                error:function(){

                }
            });

}

function  deleteData(rowId){
	 $.ajax({
         type: "post",
         data: {
      	   groupId:rowId
         },
         url: '/group/delete',
         success: function (data) {
      	   if(data == true){
      		   alert("删除成功");
      		   $("#data_table").bootstrapTable('refresh');
      	   }else{
      		   alert("删除失败");
      	   }
         }
     });
}


function initTable(dataurl,columns) {
	
    $('#data_table').bootstrapTable({
        url: dataurl,
        method:"GET",
        dataType: "json",
        contentType: "application/json;charset=UTF-8",
        striped:true,//隔行变色
        cache:false,  //是否使用缓存
        showColumns:false,// 列
        toobar:'#toolbar',
        pagination: true, //分页
        paginationLoop:false,
        paginationPreText:'上一页',
        paginationNextText:'下一页',
//        showFooter:true,//显示列脚
//        showRefresh:true,//显示刷新
        showPaginationSwitch:false,//是否显示数据条数选择框
        sortable: false,           //是否启用排序
        singleSelect: false,
        search: true, //显示搜索框
        refesh:true,
        buttonsAlign: "right", //按钮对齐方式
        showRefresh:true,//是否显示刷新按钮
        sidePagination: "server", //服务端处理分页
        pageNumber:1,
        pageSize:10,
        pageList:[10, 25, 50, 100],
        undefinedText:'--',
        uniqueId: "id", //每一行的唯一标识，一般为主键列
        queryParamsType:'',
        queryParams: queryParams,//传递参数（*）
        responseHandler:function (datas) {
        	window.rowdata=datas.list;
            return {
            	total : datas.totalCount, //总页数,前面的key必须为"total"
                rows : datas.list //行
            }
        },
//        height: tableHeight,
        columns: columns
    });

    //得到查询的参数
    function queryParams (params) {
    	//alert(JSON.stringify(params));
        var temp = {  //这里的键的名字和控制器的变量名必须一直，这边改动，控制器也需要改成一样的
            pageSize: params.pageSize,  //页面大小
            page: params.pageNumber, //页码
            groupName:params.searchText

        };
        return temp;
    };

    
 
}
   //加载数据
	function loadData(){
		$.ajax(
	            {
	                type:"POST",
	                url:"/group/dataList",
	                contentType: 'application/x-www-form-urlencoded',
	                data:{
	                	groupName:"aa",
	                	page:1,
	                	pageSize:10
	                },
	                dataType:"json",
	                success:function(json){
	                    var data = json.list;
	                    $("#data_table").bootstrapTable('load',data);
	                },
	                error:function(){
	                    alert("错误");
	                }
	            }
	        )
	       
	} 

	function tableHeight() {
		 return $(window).height() - 50;
   }
    
