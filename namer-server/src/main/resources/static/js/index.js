$(document).ready(function () {


    var columns = [{
        field: '',
        title: '唯一地址',
        align: 'center',
        formatter: function (value, row, index) {
            return row.location + '<br> (' + row.group + ', ' + row.hostName + ')';
        }
    }, {
        field: 'role',
        title: '角色',
        align: 'center'
    }, {
        field: 'updateTime',
        title: '心跳时间',
        align: 'center',
        formatter: function (value, row, index) {
            var date = new Date(value);
            return date.Format("yyyy-MM-dd hh:mm:ss");

        }
    }, {
        title: '新增操作',
        field: '',
        align: 'center',
        formatter: function (value, row, index) {
            //var data = JSON.stringify(row);
            // console.log(row);
            //var e = '<a  class="editBtn"  data-rowId=' + row.id + ' data-toggle="modal" data-target="" onclick="edit(\'' + row + '\')">编辑</button> ';
            //var d = '<a  id="delBtn" onclick="del(\'' + row.id + '\',\'' + row.groupId + '\')">删除</button> ';
            //var url = "/switch/getServerList?path=/switch/" + row.groupName + "/" + row.appName + "/" + row.name;
            //var rowId = row.id;
            var role = row.role;
            if (role == 'Master') {
                var a = '<a  class="listenBtn"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="listen(\'' + row + '\')">Redis侦听</a> ';
                var b = '<a  class="syncBtn"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="sync(\'' + row + '\')">Piper同步</a> ';
                return a + '<br>' + b;

            } else {
                var c = '<a  class="copyBtn"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="copy(\'' + row + '\')">主从复制</a> ';
                return c;
            }
        }
    }, {
        title: '当前操作',
        field: '',
        align: 'center',
        formatter: function (value, row, index) {
            var location = row.location;
            var masterName = row.masterName;
            var sentinels = row.sentinels;
            var syncPiperLocationList = row.syncPiperLocationList;
            var listenRedisTaskState = row.listenRedisTaskState;
            var syncPiperTaskStateList = row.syncPiperTaskStateList;
            var a = '';
            var b = '';
            if (masterName != null && sentinels != null && listenRedisTaskState != 'TASK_LISTEN_REDIS_ABORT') {
                a = '<a  class="stopListenBtn"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="stopRedisListen(\'' + location + '\',\'' + masterName + '\',\'' + sentinels + '\')">停止Redis侦听 (' + masterName + '-' + sentinels + ')</a> ';
                a += '<br>';
            }

            if (syncPiperLocationList != null && syncPiperTaskStateList != null) {
                for ( var i = 0; i <syncPiperLocationList.length; i++) {
                    console.log(syncPiperLocationList[i]);
                    if (syncPiperTaskStateList[i] != 'TASK_SYNC_PIPER_ABORT') {
                        b += '<a  class="stopSyncPiperBtn"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="stopSyncPiper(\'' + location + '\',\'' + syncPiperLocationList[i] + '\')">停止Piper同步(' + syncPiperLocationList[i] + ')</a> ';
                        b += '<br>';
                    }
                }
            }
            var c = '<a  class="runningInfo"  data-rowLocation=' + row.location + ' data-toggle="modal" data-target="" onclick="runningInfo(\'' + location + '\')">运行信息' + '</a> ';
            c += '<br>';
            return a + b + c;
        }
    }, {
        field: '',
        title: '侦听Redis(状态)',
        align: 'center',
        formatter: function (value, row, index) {
            var masterName = row.masterName;
            var sentinels = row.sentinels;
            var listenRedisUpdateTime = row.listenRedisUpdateTime;
            var time = null;
            if (listenRedisUpdateTime != null) {
                var date = new Date(row.listenRedisUpdateTime);
                time = date.Format("yyyy-MM-dd hh:mm:ss");
            }
            if (masterName != null) {
                return '<font color="red">Cluster Master:</font>' + masterName + ' <br><font color="red">Sentinels:</font>' + sentinels + ' <br> (' + row.listenRedisTaskState +')' + ' <br> Update:' + time;
            } else {
                return time;
            }
        }
    }, {
        field: '',
        title: '同步Pipers(状态)',
        align: 'center',
        formatter: function (value, row, index) {
            var syncPiperLocationList = row.syncPiperLocationList;
            var result = '';
            if (syncPiperLocationList != null) {
                for ( var i = 0; i <syncPiperLocationList.length; i++){
                    console.log(syncPiperLocationList[i]);
                    result += syncPiperLocationList[i] ;
                    result += ' (' + row.syncPiperTaskStateList[i] + ')' + '<br>';
                }

                var date = new Date(row.syncPiperUpdateTime);
                var time = date.Format("yyyy-MM-dd hh:mm:ss");
                return result + ' <br>  Update:' + time;
            } else {
                return '';
            }
        }
    }, {
        field: 'copyMasterLocation',
        title: '主从复制(MasterLocation)',
        align: 'center'
    }
    ];

    initTable("/index/piperList", columns);

    $("#listern-saveBtn").click(listenSave);
    $("#sync-saveBtn").click(syncSave);
    $("#copy-saveBtn").click(copySave);
});

function listen(row) {
    $("#listenModel").modal("show");
}

function sync(row) {
    $("#syncModel").modal("show");
}

function copy(row) {
    $("#copyModel").modal("show");
}

function stopRedisListen(location, materName, sentinels) {
    if (checkSentinelsReg(sentinels) == null) {
        alert("sentinels 格式不对，格式: 127.0.0.1:6379,127.0.0.1:6378");
        return;
    }
    var url = "/index/stopRedisListen";
    $.ajax({
        type: "post",
        data: {
            location: location,
            masterName: materName,
            sentinels: sentinels
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
            }
        }
    });
}

function stopSyncPiper(location, syncPiperLocation) {
    var url = "/index/stopSyncPiper";

    $.ajax({
        type: "post",
        data: {
            location: location,
            syncPiperLocation: syncPiperLocation
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
            }

        }
    });
}

function runningInfo(location) {
    var url = "/index/runningInfo";

    $.ajax({
        type: "post",
        data: {
            location: location        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert(data.data);
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
            }

        }
    });
}

$(document).on('click', '.listenBtn', function () {
    var location = $(this).attr('data-rowLocation');
    $("#listen-location").val(location);
})

$(document).on('click', '.syncBtn', function () {
    var location = $(this).attr('data-rowLocation');
    $("#sync-location").val(location);
})

$(document).on('click', '.copyBtn', function () {
    var location = $(this).attr('data-rowLocation');
    $("#copy-location").val(location);
})


function listenSave() {
    var srcLocation = $("#listen-location").val().trim();
    var masterName = $("#masterName").val().trim();
    var sentinels = $("#sentinels").val().trim();
    var password = $("#password").val().trim();
    if (checkSentinelsReg(sentinels) == null) {
        alert("sentinels 格式不对，格式: 127.0.0.1:6379,127.0.0.1:6378");
        return;
    }
    if (masterName == '') {
        alert("sentinels 格式不对，格式: 127.0.0.1:6379,127.0.0.1:6378");
        return;
    }

    var url = "/index/listenRedisSave";
    $.ajax({
        type: "post",
        data: {
            location: srcLocation,
            masterName: masterName,
            sentinels: sentinels,
            password: password
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
            }

        }
    });
}
function syncSave() {
    var srcLocation = $("#sync-location").val().trim();
    var syncPiperLocation = $("#sync-descLocation").val().trim();
    var url = "/index/syncPiperSave";

    $.ajax({
        type: "post",
        data: {
            location: srcLocation,
            syncPiperLocation: syncPiperLocation
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
            }
        }
    });
}
function copySave() {

    var listenLocation = $("#listen-location").val();
    var redisUri = $("#redisUri").val();

    if (checkRedisUriReg(redisUri) == null) {
        alert("redisUri 格式不对，格式: redis://127.0.0.1:6379");
        return;
    }


    var url = "/index/listenSave";

    $.ajax({
        type: "post",
        data: {
            location: listenLocation,
            redisUri: redisUri
        },
        url: url,
        success: function (data) {
            if (data.code == 0) {
                alert("操作成功");
                $("#data_table").bootstrapTable('refresh');
            } else {
                alert("操作失败:" + data.remark);
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
    
