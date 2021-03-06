/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jajitech.agenci.controller;

import com.google.gson.Gson;
import com.jajitech.agenci.helper.Emailer;
import com.jajitech.agenci.helper.FileUploadManager;
import com.jajitech.agenci.helper.Hasher;
import com.jajitech.agenci.helper.ResponseParser;
import com.jajitech.agenci.model.WorkerModel;
import com.jajitech.agenci.repository.WorkerRepository;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Lukman Jaji <lukman@lukmanjaji.com>
 */
@RestController
@RequestMapping("/agenci/worker/")
public class WorkerController {
    
    @Autowired
    WorkerRepository workers;
    
    @Autowired
    Emailer mailer;
    
    @Autowired
    Gson gson;
    
    String actionMessage = "", savedID = "";
    
    @Autowired
    FileUploadManager uploader;
    
    @Autowired
    ResponseParser parser;
    
    @Autowired
    Hasher hasher;
    
    @PostMapping(path="addWorker")
    public String save(@RequestParam("name") String name, @RequestParam("email") String email, 
            @RequestParam("address") String address, @RequestParam("phone") String phone, 
            @RequestParam("photoFile") MultipartFile photoFile, @RequestParam("idtype") String idtype,
            @RequestParam("idnumber") String idnumber,
            @RequestParam("dob") String dob, @RequestParam("agencyId") String agencyId)
    {
        try
        {
        actionMessage = "";
        boolean g =  workers.existsByEmail(email);
        if(g == true)
        {
            WorkerModel w = new WorkerModel();
                w.setId((long) 0);
                w.setAgencyID("error#A worker with the provided email already exists.");
                return gson.toJson(w);
        }
        
        WorkerModel worker = new WorkerModel();
        WorkerModel ag = null;
        try{Date date1=new SimpleDateFormat("dd/MM/yyyy").parse(dob);
        System.out.println(date1);
        worker.setDob(date1);}catch(Exception er){}
        worker.setIdNumber(idnumber);
        worker.setIdType(idtype);
        worker.setIsPhotoUploaded(false);
        worker.setWorkerAddress(address);
        worker.setWorkerEmail(email);
        worker.setWorkerPhone(phone);
        worker.setWorkerName(name);
        try{worker.setAgencyID(hasher.hashPassword(agencyId));}catch(Exception er){}
        boolean success = false;
        try
        {
            ag = workers.save(worker);
            savedID = ag.getId()+"";
            success = true;
           ag.setAgencyID("ok#Worker registered.");
            
        }catch(Exception er){ag.setAgencyID("error#Error registering worker.");}
        if(success == true)
        {
            System.out.println("sending mail...");
            String r = mailer.sendVerificationCodeForWorker(savedID);
            System.out.println("mail outcome: "+r);
        }
        if(success == true && photoFile != null)
        {
            boolean uploaded = uploader.doUpload("worker", photoFile, savedID);
            if(uploaded == true)
            {
                workers.updatePhoto(savedID);
                ag.setIsPhotoUploaded(true);
            }
            return gson.toJson(ag);
        }
        }
        catch(Exception er)
        {
            er.printStackTrace();
            WorkerModel w = new WorkerModel();
                w.setId((long) 0);
                w.setAgencyID("error#Error registering worker.");
                return gson.toJson(w);
        }
        return "";
    }
    
    @PostMapping("login")
    public String LoginWorker(@RequestParam("u") String email, @RequestParam("p") String password)
    {
        try
        {
            WorkerModel d = workers.findByEmail(URLDecoder.decode(email, "utf-8"));
            String v = d.getAgencyID();
            boolean t = hasher.verifyHash(password, d.getAgencyID());
            if(t == true)
            {
                d.setAgencyID("");
                return gson.toJson(d);
            }
            else
            {
                WorkerModel w = new WorkerModel();
                w.setId((long) 0);
                System.out.println(gson.toJson(w));
                return gson.toJson(w);
            }
        }
        catch(Exception er)
        {
            WorkerModel w = new WorkerModel();
            w.setId((long) 0);
            System.out.println(gson.toJson(w));
            return gson.toJson(w);
        }
    }
    
    @PostMapping("setUp")
    public String setUp(@RequestParam("workerId") String workerId,@RequestParam("p") String p)
    {
        try
        {
            WorkerModel wm = new WorkerModel();
            System.out.println(workerId + " "+ p);
            try{p = hasher.hashPassword(p);}catch(Exception er){}
            String id = workers.verifyWorkerEmail(URLDecoder.decode(workerId, "utf-8"));
            int x = workers.u_p(p, id);
            if(x > 0)
            {
                System.out.println("credentials updated");
                return gson.toJson(parser.parseResponse("u_p", "true"));
            }
            else
            {
                System.out.println("error updating credentials for worker");
                return gson.toJson(parser.parseResponse("u_p", "false"));
            }
        }
        catch(Exception er)
        {
            er.printStackTrace();
            return gson.toJson(parser.parseResponse("u_p", "false"));
        }
    }
    
    @PostMapping(path="updateWorker")
    public String update(@RequestParam("name") String name, @RequestParam("email") String email, 
            @RequestParam("address") String address, @RequestParam("phone") String phone, 
             @RequestParam("idtype") String idtype,
            @RequestParam("idnumber") String idnumber,@RequestParam("id") String id,
            @RequestParam("dob") String dob)
    {
        WorkerModel worker = new WorkerModel();
        try{Date date1=new SimpleDateFormat("dd/MM/yyyy").parse(dob); 
        worker.setDob(date1);}catch(Exception er){}
        worker.setIdNumber(idnumber);
        worker.setIdType(idtype);
        worker.setWorkerAddress(address);
        worker.setWorkerEmail(email);
        worker.setWorkerPhone(phone);
        worker.setWorkerName(name);
        //worker.setAgencyID(agencyid);
        worker.setId(Long.parseLong(id));
        try
        {
            WorkerModel ag = workers.save(worker);
            savedID = ag.getId()+"";
            actionMessage = "Worker updated.";
        }catch(Exception er){actionMessage = "Error updating worker";}
        return gson.toJson(parser.parseResponse("worker_update", actionMessage));
    }
    
    @PostMapping(path="deleteWorker")
    public String delete(@RequestParam("agency_id") String agency_id, 
            @RequestParam("worker_id") String worker_id)
    {
        try
        {
            workers.deleteWorker(worker_id, agency_id);
            actionMessage = "Worker deleted";
        }
        catch(Exception er)
        {
            actionMessage = "Error deleting worker";
        }
        return gson.toJson(parser.parseResponse("deleting_worker", actionMessage));
    }
    
    @GetMapping(path="listWorkersInAgency")
    public String getAllAgencyWorkers(@RequestParam("agency_id") String agency_id)
    {
        return gson.toJson(workers.listAllWorkersForAgency(agency_id));
    }
    
    @GetMapping(path="getWorkerInfo")
    public String getWorkerInfo(@RequestParam("worker_id") String worker_id)
    {
        return gson.toJson(workers.findById(Long.parseLong(worker_id)));
    }
    
    @PostMapping(path="verifyWorker")
    public boolean verifyWorker(@RequestParam("workerId") String workerId,
            @RequestParam("code") String code)
    {
        boolean verifyWorkerCode = workers.veryfiyWorkerCode(code, workerId);
        if(verifyWorkerCode == true)
        {
            int x = workers.verifyWorker(code, workerId);
            if(x > 0)
            {
                mailer.sendInfoEmail("post_reg", workerId, "");
                return true;
            }
            else
            {
                return false;
            }
            
        }
        else
        {
            return false;
        }
    }
    
    @PostMapping(path="sendPassReset")
    public String sendPassCodeForWorker(@RequestParam("email") String email)
    {
        try{email = URLDecoder.decode(email, "utf-8");}catch(Exception er){}
        WorkerModel wm = workers.findByEmail(email);
        String id = "";
        try
        {
            id = ""+wm.getId();
        }
        catch(Exception er)
        {
            String t = gson.toJson(parser.parseResponse("email_verify", "No account associated with email provided"));
            return t;
        }
        
        if(id == null)
        {
            String t = gson.toJson(parser.parseResponse("email_verify", "No account associated with email provided"));
            System.out.println(t);
            return t;
        }
        String r = mailer.sendPassCodeForWorker(id);
        System.out.println(r);
        if(r != null && !r.contains("error"))
        {
            return gson.toJson(parser.parseResponse("pass_reset_code", r));
        }
        else
        {
            return gson.toJson(parser.parseResponse("pass_reset_code", "Error encountered"));
        }
    }
   
    
    
    
    
}
