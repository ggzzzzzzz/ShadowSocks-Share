package com.example.ShadowSocksShare.web;


import com.example.ShadowSocksShare.domain.ShadowSocksDetailsEntity;
import com.example.ShadowSocksShare.domain.ShadowSocksEntity;
import com.example.ShadowSocksShare.service.ShadowSocksSerivce;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class MainController {
	@Autowired
	private ShadowSocksSerivce shadowSocksSerivceImpl;

	/**
	 * 首页
	 */
	@RequestMapping("/")
	public String index(@PageableDefault(page = 0, size = 50, sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable, Model model) {
		List<ShadowSocksEntity> ssrList = shadowSocksSerivceImpl.findAll(pageable);
		List<ShadowSocksDetailsEntity> ssrdList = new ArrayList<>();
		for (ShadowSocksEntity ssr : ssrList) {
			ssrdList.addAll(ssr.getShadowSocksSet());
		}
		// ssr 信息
		model.addAttribute("ssrList", ssrList);
		// ssr 明细信息
		model.addAttribute("ssrdList", ssrdList);
		return "index";
	}

	/**
	 * SSR 订阅地址
	 *
	 * @return
	 */
	@RequestMapping("/subscribe")
	@ResponseBody
	public String subscribe(boolean valid, @PageableDefault(page = 0, size = 50, sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable) {
		List<ShadowSocksEntity> ssrList = shadowSocksSerivceImpl.findAll(pageable);
		String ssrLink = shadowSocksSerivceImpl.toSSLink(ssrList, valid);
		return StringUtils.isNotBlank(ssrLink) ? ssrLink : "无有效 SSR 连接，请稍后重试！";
	}

	/**
	 * 二维码
	 */
	@RequestMapping(value = "/createQRCode", produces = {MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	@ResponseBody
	public ResponseEntity<byte[]> createQRCode(String text, int width, int height) throws IOException, WriterException {
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(MediaType.IMAGE_PNG_VALUE)).body(shadowSocksSerivceImpl.createQRCodeImage(text, width, height));
	}


	@Bean
	public ErrorPageRegistrar errorPageRegistrar(){
		return new MyErrorPageRegistrar();
	}

	private static class MyErrorPageRegistrar implements ErrorPageRegistrar {

		@Override
		public void registerErrorPages(ErrorPageRegistry registry) {
			registry.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
		}

	}
}
