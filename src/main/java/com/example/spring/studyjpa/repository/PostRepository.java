package com.example.spring.studyjpa.repository;

import com.example.spring.studyjpa.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {



}
