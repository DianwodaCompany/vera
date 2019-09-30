$(document).ready(function () {


    var columns = [{
        field: 'id',
        title: 'ID',
        align: 'center'
    }, {
        field: 'actionTaskEnum',
        title: 'Action任务类型',
        align: 'center'
    }, {
        field: '',
        title: '源Piper',
        align: 'center',
        formatter: function (value, row, index) {
            var location = row.srcPiperData.location;
            var group = row.srcPiperData.group;
            var id = row.srcPiperData.piperId;
            return location + '<br>' + group + '<br>' + (id == 0 ? 'Master' : 'Slave');
        }
    }, {
        field: 'operand',
        title: '操作数',
        align: 'center'
    }, {
        field: 'createTime',
        title: '创建时间',
        align: 'center',
        formatter: function (value, row, index) {
            var date = new Date(value);
            return date.Format("yyyy-MM-dd hh:mm:ss");

        }
    }, {
        field: 'updateTime',
        title: '更新时间',
        align: 'center',
        formatter: function (value, row, index) {
            var date = new Date(value);
            return date.Format("yyyy-MM-dd hh:mm:ss");

        }
    }, {
        field: 'actionStateEnum',
        title: '状态',
        align: 'center'
    }, {
        title: '操作',
        field: '',
        align: 'center',
        formatter: function (value, row, index) {
            var actionState = row.actionStateEnum;
            var button1 = '';
            var button2 = '';
            if (actionState == 'DEFAULT') {
                button1 = '<a  class="actionAgreeBtn"  data-toggle="modal" data-target="" onclick="actionAgree(\'' + row.id + '\')">同意</a> ';

            } else if (actionState == 'AGREE') {
                button1 = '<a  class="actionStartBtn"  data-toggle="modal" data-target="" onclick="actionStart(\'' + row.id + '\')">启动</a> ';
            } else if (actionState == 'FINISH') {
                button1 = '<front color="red">完成</front>';
            } else if (actionState == 'REJECT') {
                button1 = '<front color="red">完成</front>';
            }
            if (actionState == 'DEFAULT') {
                button2 = '<a  class="actionRejectBtn"  data-toggle="modal" data-target="" onclick="actionReject(\'' + row.id + '\')">拒绝</a> ';
            }
            return button1 + button2;
        }
    }
    ];


    initTable("/action/actionList", columns);

});

function actionAgree(id) {

    var url = "/action/actionAgree";

    $.ajax({
        type: "post",
        data: {
            id: id
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败: " + data.remark);
            }

        }
    });
}

function actionStart(id) {

    var url = "/action/actionStart";

    $.ajax({
        type: "post",
        data: {
            id: id
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败: " + data.remark);
            }
        }
    });
}


function actionReject(id) {

    var url = "/action/actionReject";

    $.ajax({
        type: "post",
        data: {
            id: id
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败: " + data.remark);
            }

        }
    });
}

function initTable(dataurl, columns) {

    $('#data_table').bootstrapTable({
        url: dataurl,
        method: "GET",
        dataType: "json",
        contentType: "application/json;charset=UTF-8",
        striped: true,//隔行变色
        cache: false,  //是否使用缓存
        showColumns: false,// 列
        toobar: '#toolbar',
        pagination: true, //分页
        paginationLoop: false,
        paginationPreText: '上一页',
        paginationNextText: '下一页',
//        showFooter:true,//显示列脚
//        showRefresh:true,//显示刷新
        showPaginationSwitch: false,//是否显示数据条数选择框
        sortable: false,           //是否启用排序
        singleSelect: false,
        search: true, //显示搜索框
        refesh: true,
        buttonsAlign: "right", //按钮对齐方式
        showRefresh: true,//是否显示刷新按钮
        sidePagination: "server", //服务端处理分页
        pageNumber: 1,
        pageSize: 10,
        pageList: [10, 25, 50, 100],
        undefinedText: '--',
        uniqueId: "id", //每一行的唯一标识，一般为主键列
        queryParamsType: '',
        queryParams: queryParams,//传递参数（*）
        responseHandler: function (datas) {
            window.rowdata = datas.list;
            return {
                total: datas.totalCount, //总页数,前面的key必须为"total"
                rows: datas.list //行
            }
        },
//        height: tableHeight,
        columns: columns
    });

    //得到查询的参数
    function queryParams(params) {
        //alert(JSON.stringify(params));
        var temp = {  //这里的键的名字和控制器的变量名必须一直，这边改动，控制器也需要改成一样的
            pageSize: params.pageSize,  //页面大小
            page: params.pageNumber, //页码
            name: params.searchText

        };
        return temp;
    };


}

function tableHeight() {
    return $(window).height() - 50;
}
    
