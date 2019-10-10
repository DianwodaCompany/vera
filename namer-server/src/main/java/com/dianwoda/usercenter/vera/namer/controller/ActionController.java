package com.dianwoda.usercenter.vera.namer.controller;
import com.dianwoda.usercenter.vera.common.Pagination;
import com.dianwoda.usercenter.vera.namer.dto.Action;
import com.dianwoda.usercenter.vera.namer.dto.ActionInfoDTO;
import com.dianwoda.usercenter.vera.namer.routeinfo.ActionManager;
import com.dianwoda.usercenter.vera.namer.tools.DefaultAdminExtImpl;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author seam
 */
@Controller
@RequestMapping(value = "/action")
public class ActionController {
  private DefaultAdminExtImpl defaultAdminExtImpl = new DefaultAdminExtImpl();

  @RequestMapping("/list")
  public String index() {
    return "action/list";
  }

  @RequestMapping("actionList")
  public @ResponseBody
  Object actionList(@RequestParam(defaultValue = "1", required = false) int page,
                    @RequestParam(defaultValue = "1000", required = false) int pageSize,
                    @RequestParam(name = "name", required = false) String searchText) {
    ActionManager actionManager = ActionManager.getInstance();
    Pagination<ActionInfoDTO> pagination = new Pagination(page, pageSize);
    List<ActionInfoDTO> actionInfoDTOList = new ArrayList<ActionInfoDTO>();

    for (Map.Entry<Integer, Action> entry : actionManager.getActionMap().entrySet()) {
      ActionInfoDTO actionInfo = new ActionInfoDTO(entry.getKey(), entry.getValue());
      actionInfoDTOList.add(actionInfo);
    }
    actionInfoDTOList = actionInfoDTOList.stream().filter(a ->
            StringUtils.isEmpty(searchText) ? true : a.getSrcPiperData().getLocation().equals(searchText))
            .sorted(Comparator.comparing(ActionInfoDTO::getId).reversed())
            .collect(Collectors.toList());

    pagination.setList(actionInfoDTOList);
    pagination.setTotalCount(actionInfoDTOList.size());
    pagination.setCurrentPage(1);
    return pagination;
  }

  @RequestMapping("actionAgree")
  public @ResponseBody
  Object actionAgree(@RequestParam(name = "id") Integer id) {
    Preconditions.checkNotNull(id);
    return this.defaultAdminExtImpl.actionAgree(id);
  }

  @RequestMapping("actionStart")
  public @ResponseBody
  Object actionStart(@RequestParam(name = "id") Integer id) {
    Preconditions.checkNotNull(id);
    return this.defaultAdminExtImpl.actionStart(id);
  }

  @RequestMapping("actionReject")
  public @ResponseBody
  Object actionReject(@RequestParam(name = "id") Integer id) {
    Preconditions.checkNotNull(id);
    return this.defaultAdminExtImpl.actionReject(id);
  }
}
