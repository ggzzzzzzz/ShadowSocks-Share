package com.example.ShadowSocksShare.service.impl;

import com.example.ShadowSocksShare.domain.ShadowSocksDetailsRepository;
import com.example.ShadowSocksShare.domain.ShadowSocksRepository;
import com.example.ShadowSocksShare.domain.ShadowSocksDetailsEntity;
import com.example.ShadowSocksShare.domain.ShadowSocksEntity;
import com.example.ShadowSocksShare.service.ShadowSocksCrawlerService;
import com.example.ShadowSocksShare.service.ShadowSocksSerivce;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 1. 爬取 目标网站 SS 信息
 * 2. SS 信息入库
 * 3. 前台页面展示信息
 */
@Slf4j
@Service
public class ShadowSocksSerivceImpl implements ShadowSocksSerivce {
	@Autowired
	private ShadowSocksRepository shadowSocksRepository;
	@Autowired
	private ShadowSocksDetailsRepository shadowSocksDetailsRepository;

	/**
	 * 1. 爬取 SS 并入库
	 * 2. SS 信息入库
	 */
	@Override
	@Transactional
	public void crawlerAndSave(ShadowSocksCrawlerService service) {
		// 1. 爬取 目标网站 SS 信息
		long begin = System.currentTimeMillis();
		ShadowSocksEntity socksEntity = service.getShadowSocks();
		long end = System.currentTimeMillis();
		log.debug("抓取执行时间： {} 毫秒", end - begin);

		// 2. SS 信息入库
		if (socksEntity != null) {
			// shadowSocksRepository.deleteByTargetURL(socksEntity.getTargetURL());
			ShadowSocksEntity entity = shadowSocksRepository.findByTargetURL(socksEntity.getTargetURL());
			if (entity != null)
				shadowSocksRepository.delete(entity);

			shadowSocksRepository.save(socksEntity);
			log.debug("入库执行时间： {} 毫秒", System.currentTimeMillis() - end);
		}
	}

	/**
	 * 3. 查询 SS 信息
	 */
	@Override
	public List<ShadowSocksEntity> findAll(Pageable pageable) {
		Page<ShadowSocksEntity> entities = shadowSocksRepository.findAll(pageable);
		return entities.getContent();
	}

	/**
	 * 3. 生成 SSR 连接
	 */
	@Override
	public String toSSLink(List<ShadowSocksEntity> entities, boolean valid) {
		if (!entities.isEmpty()) {
			StringBuilder link = new StringBuilder();
			for (ShadowSocksEntity entity : entities) {
				link.append(entity.getLink(valid));
			}
			return link.toString();
		}
		return "";
	}

	/**
	 * SS 有效性检查，获取 SS 信息，判断端口有效性，并更新数据
	 */
	@Override
	@Transactional
	public void checkValid() {
		List<ShadowSocksEntity> entityList = shadowSocksRepository.findAll();
		for (ShadowSocksEntity shadowSocksEntity : entityList) {
			for (ShadowSocksDetailsEntity shadowSocksDetailsEntity : shadowSocksEntity.getShadowSocksSet()) {
				boolean _valid = ShadowSocksCrawlerService.isReachable(shadowSocksDetailsEntity);
				// 如果检测结果与库中数据 不一致，则更新数据
				if (_valid != shadowSocksDetailsEntity.isValid()) {
					shadowSocksDetailsEntity.setValid(_valid);
					shadowSocksDetailsEntity.setValidTime(new Date());
					shadowSocksDetailsRepository.save(shadowSocksDetailsEntity);
				}
			}
		}
	}

	/**
	 * 生成二维码
	 */
	@Override
	public byte[] createQRCodeImage(String text, int width, int height) throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

		ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
		byte[] pngData = pngOutputStream.toByteArray();
		return pngData;
	}
}
