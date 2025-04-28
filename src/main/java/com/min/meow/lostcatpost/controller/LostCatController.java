package com.min.meow.lostcatpost.controller;


import com.min.meow.lostcatpost.domain.LostCatPostEntity;
import com.min.meow.lostcatpost.service.LostCatPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat")
public class LostCatController {

    private final LostCatPostService lostCatPostService;

    @GetMapping("/posts")
    public String getAllLostCatPosts(Model model,
                                     @RequestParam int page,
                                     @RequestParam int size){

        Pageable pageable = PageRequest.of(page, size, Sort.by("create_at").descending());
        Page<LostCatPostEntity> allLostCatPosts = lostCatPostService.getAllLostCatPosts(pageable);

        model.addAttribute("allLostCatPost", allLostCatPosts);
        model.addAttribute("allLostCatPosts", allLostCatPosts.getContent());
        model.addAttribute("currentPage", allLostCatPosts.getNumber());
        model.addAttribute("totalPages", allLostCatPosts.getTotalPages());
        model.addAttribute("totalItems", allLostCatPosts.getTotalElements());

        return "lostCatPosts";
    }
}
