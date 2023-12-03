package com.programmers.springboard.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.programmers.springboard.entity.Member;
import com.programmers.springboard.entity.Post;
import com.programmers.springboard.exception.MemberNotFoundException;
import com.programmers.springboard.exception.PostNotFoundException;
import com.programmers.springboard.repository.MemberRepository;
import com.programmers.springboard.repository.PostCustomRepository;
import com.programmers.springboard.repository.PostRepository;
import com.programmers.springboard.request.PostCreateRequest;
import com.programmers.springboard.request.PostSearchRequest;
import com.programmers.springboard.request.PostUpdateRequest;
import com.programmers.springboard.response.PostResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostService {

	private final PostRepository postRepository;
	private final PostCustomRepository postCustomRepository;
	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public Page<PostResponse> getPosts(PostSearchRequest request, Pageable pageable) {
		Page<Post> posts = postCustomRepository.findPostsByCustomCondition(request, pageable);
		return posts.map(PostResponse::of);
	}

	@Transactional(readOnly = true)
	public PostResponse getPostById(Long id) {
		return postRepository.findById(id)
			.map(PostResponse::of)
			.orElseThrow(PostNotFoundException::new);
	}

	public PostResponse createPost(PostCreateRequest request) {
		Member member = memberRepository.findById(request.memberId()).orElseThrow(MemberNotFoundException::new);
		Post post = postRepository.save(request.toEntity(member));
		return PostResponse.of(post);
	}

	public PostResponse updatePost(Long id, PostUpdateRequest request) {
		Post post = postRepository.findById(id)
			.orElseThrow(PostNotFoundException::new);
		post.changePostTitleAndContent(request.title(), request.content());
		return PostResponse.of(post);
	}

	public void deletePosts(List<Long> ids) {
		List<Post> posts = postRepository.findAllById(ids);
		postRepository.deleteAll(posts);
	}
}
