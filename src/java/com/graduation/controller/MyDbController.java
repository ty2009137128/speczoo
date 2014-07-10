package com.graduation.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.graduation.common.Pager;
import com.graduation.model.AjaxObj;
import com.graduation.model.MydbElement;
import com.graduation.model.User;
import com.graduation.service.IMydbService;
import com.graduation.util.JsonUtil;
import com.graduation.util.ParameterHandleUtil;
import com.graduation.util.ResultSetHandleUtil;
import com.graduation.web.SessionContext;

@Controller
@RequestMapping("/mydb")
public class MyDbController {

    @Inject
    private IMydbService mydbService;

    @RequestMapping(value = "/mydbUpload", method = RequestMethod.GET)
    public String mydb() {
        return "mydb/mydb";
    }

    @RequestMapping(value = "/mydbUpload", method = RequestMethod.POST)
    @ResponseBody
    public String mydbUpload(MultipartFile file, String tableName, HttpServletRequest request, Model model) {
        //error中记录了出错数据的行数和信息
        ArrayList<String> error = new ArrayList<String>();
        AjaxObj ajax = new AjaxObj();
        if (tableName == null || "".equals(tableName.trim())) {
            ajax.setResult(0);
            ajax.setMessage("The Table'name is null.....");
            return JsonUtil.getInstance().obj2json(ajax);
        }
        if (ParameterHandleUtil.isNumber(tableName)) {
            ajax.setResult(0);
            ajax.setMessage("TableName must contain char....");
            return JsonUtil.getInstance().obj2json(ajax);
        }
        HttpSession session = SessionContext.getSession(request.getParameter("sessionId"));
        User loginUser = (User) session.getAttribute("loginUser");
        try {
            tableName = tableName.trim();
            long rowNum = this.mydbService.insertBach(file.getInputStream(), tableName, error);
            if(rowNum==0){
                ajax.setResult(0);
                ajax.setMessage("update number is empty......");
                ajax.setObj(error);
                return JsonUtil.getInstance().obj2json(ajax);
            }
            MydbElement mydb = new MydbElement();
            mydb.setRowNum(rowNum);
            mydb.setTableName(tableName);
            mydb.setUid(loginUser.getId());
            this.mydbService.insertMydb(mydb);

        } catch (IOException e) {
            e.printStackTrace();
            return JsonUtil.getInstance().obj2json(ajax);
        }
        ajax.setResult(1);
        ajax.setObj(error);
        System.out.println(error);
        return JsonUtil.getInstance().obj2json(ajax);

    }

    @RequestMapping(value = "/listMydbInfo")
    @ResponseBody
    public List<MydbElement> getMydbElementList(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");

        return this.mydbService.list(loginUser.getId());
    }


    @RequestMapping("/deleteTable")
    public String deleteByTableName(@RequestParam("id") Integer id) {
        //通过指定的表id来删除，第一参数为uid，表示不是删除用户所建的所有表
        int result = this.mydbService.deleteTable(null, id);
        if (result == 0) {
            //未做任何改变
        }
        return "mydb/mydb";
    }

    @RequestMapping("/clearAllTable")
    public String clearAll(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        int result = this.mydbService.deleteTable(loginUser.getId(), null);
        return "mydb/mydb";
    }

    @RequestMapping("/showTable/{tableName}")
    public String showTable(@PathVariable("tableName") String tableName, Model model) {

        if (null == tableName || "".equals(tableName.trim()))
            return "mydb/mydb";
        //验证表格在mydb_user中是否存在，用户只能查看自己建的表
        MydbElement mydb = this.mydbService.getMydbElementByTableName(tableName);
        if (mydb == null) {
            model.addAttribute("error", "您没有创建该表.....");
            return "mydb/mydb";
        }
        Pager<Map<String, Object>> pager = this.mydbService.findTableData(tableName);
        Set<String> names = pager.getDatas().get(0).keySet();
        model.addAttribute("names", names);
        model.addAttribute("pager", pager);
        model.addAttribute("tableName", tableName);
        return "mydb/mydb";
    }


    @RequestMapping("/download/{tableName}")
    public String downLoad(@PathVariable("tableName") String tableName, HttpServletResponse response, Model model) {
        if (null == tableName || "".equals(tableName.trim())) {
            model.addAttribute("error", "表格名不能为空空....");
            return "mydb/mydb";
        }
        //验证表格在mydb_user中是否存在，用户只能查看自己建的表
        MydbElement mydb = this.mydbService.getMydbElementByTableName(tableName);
        if (mydb == null) {
            model.addAttribute("error", "您没有创建该表.....");
            return "mydb/mydb";
        }

        return "redirect:/mydb/data/download/" + tableName;
    }

    @RequestMapping("/data/download/{tableName}")
    public void down(@PathVariable("tableName") String tableName, HttpServletResponse response) {
        try {
            List<Map<String, Object>> ls = this.mydbService.listTableData(tableName);
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream;charset=UTF-8");
            String fileName = new String(tableName.getBytes("UTF-8"), "ISO-8859-1");
            fileName = fileName + ".txt";
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            ResultSetHandleUtil.listMap2OutputStream(ls, response.getOutputStream());
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @RequestMapping("/test")
    @ResponseBody
    public List<Map<String, Object>> test(@RequestParam("tableName") String tableName, Model model) {
        //return this.mydbService.findTableData(tableName);
        return this.mydbService.listTableData(tableName);
    }
}
