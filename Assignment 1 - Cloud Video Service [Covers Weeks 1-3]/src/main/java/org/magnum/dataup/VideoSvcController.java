/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import retrofit.http.Multipart;
import retrofit.http.Streaming;

@Controller
public class VideoSvcController {

	private static final AtomicLong currentId = new AtomicLong(0L);

	private Map<Long, Video> videos = new HashMap<Long, Video>();

	private VideoFileManager videoDataMgr;

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it to
	 * something other than "AnEmptyController"
	 * 
	 * 
	 * ________ ________ ________ ________ ___ ___ ___ ________ ___ __ |\
	 * ____\|\ __ \|\ __ \|\ ___ \ |\ \ |\ \|\ \|\ ____\|\ \|\ \ \ \ \___|\ \
	 * \|\ \ \ \|\ \ \ \_|\ \ \ \ \ \ \ \\\ \ \ \___|\ \ \/ /|_ \ \ \ __\ \ \\\
	 * \ \ \\\ \ \ \ \\ \ \ \ \ \ \ \\\ \ \ \ \ \ ___ \ \ \ \|\ \ \ \\\ \ \ \\\
	 * \ \ \_\\ \ \ \ \____\ \ \\\ \ \ \____\ \ \\ \ \ \ \_______\ \_______\
	 * \_______\ \_______\ \ \_______\ \_______\ \_______\ \__\\ \__\
	 * \|_______|\|_______|\|_______|\|_______|
	 * \|_______|\|_______|\|_______|\|__| \|__|
	 * 
	 * 
	 */

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}

	public void saveSomeVideo(Video v, MultipartFile videoData)
			throws IOException {
		videoDataMgr.saveVideoData(v, videoData.getInputStream());
	}

	public void serveSomeVideo(Video v, HttpServletResponse response)
			throws IOException {
		videoDataMgr.copyVideoData(v, response.getOutputStream());
	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody
	Video addVideo(@RequestBody Video v) {
		save(v);

		v.setDataUrl(getUrlBaseForLocalServer() + "/video/" + v.getId()
				+ "/data");

		return v;
	}

	@Multipart
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody
	VideoStatus setVideoData(@PathVariable("id") long id,
			final @RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) throws IOException {

		videoDataMgr = VideoFileManager.get();
		VideoStatus state = new VideoStatus(VideoState.PROCESSING);

		if (videos.get(id) != null) {
			saveSomeVideo(videos.get(id), videoData);

			state.setState(VideoStatus.VideoState.READY);

		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}

		return state;
	}

	@Streaming
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public @ResponseBody
	void getData(@PathVariable("id") long id,
			HttpServletResponse response) throws IOException {

		if (videos.get(id) != null) {
			Video v = videos.get(id);
			serveSomeVideo(v, response);
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);

		}
	}

}
