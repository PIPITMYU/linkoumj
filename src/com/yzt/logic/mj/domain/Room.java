/*
 * Powered By [up72-framework]
 * Web Site: http://www.up72.com
 * Since 2006 - 2017
 */

package com.yzt.logic.mj.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 
 * 
 * @author up72
 * @version 1.0
 * @since 1.0
 */
public class Room implements java.io.Serializable {

	private Long id;
	private Integer roomId;
	private Long createId;
	private String createTime;
	private Integer isPlaying;
	private Long userId1;
	private Long userId2;
	private Long userId3;
	private Long userId4;
	private Integer roomType;//
	private Integer circleNum; // 圈数
	private Integer clubId;// 俱乐部id
	private String ip;// 当前房间所在服务器的ip
	//特有的规则
	private Integer scoreType;// 计分方式
	private Integer playerNum;
	private Integer sanQiSuanJia;
	private Integer danDiaoSuanJia;
	private Integer gangScore;
	private Integer hongZhongBao;
	private Integer guaDaFeng;
	private Integer buJiaBuHu;
	private Integer playType;
	private Integer qiangGangHu;
	

	public int hashCode() {
		return new HashCodeBuilder().append(getId()).toHashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof Room == false)
			return false;
		if (this == obj)
			return true;
		Room other = (Room) obj;
		return new EqualsBuilder().append(getId(), other.getId()).isEquals();
	}

	public Long getUserId1() {
		return userId1;
	}

	public void setUserId1(Long userId1) {
		this.userId1 = userId1;
	}

	public Long getUserId2() {
		return userId2;
	}

	public void setUserId2(Long userId2) {
		this.userId2 = userId2;
	}

	public Long getUserId3() {
		return userId3;
	}

	public void setUserId3(Long userId3) {
		this.userId3 = userId3;
	}

	public Long getUserId4() {
		return userId4;
	}

	public void setUserId4(Long userId4) {
		this.userId4 = userId4;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getRoomId() {
		return roomId;
	}

	public void setRoomId(Integer roomId) {
		this.roomId = roomId;
	}

	public Long getCreateId() {
		return createId;
	}

	public void setCreateId(Long createId) {
		this.createId = createId;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public Integer getIsPlaying() {
		return isPlaying;
	}

	public void setIsPlaying(Integer isPlaying) {
		this.isPlaying = isPlaying;
	}

	public Integer getRoomType() {
		return roomType;
	}

	public void setRoomType(Integer roomType) {
		this.roomType = roomType;
	}

	public Integer getCircleNum() {
		return circleNum;
	}

	public void setCircleNum(Integer circleNum) {
		this.circleNum = circleNum;
	}

	public Integer getClubId() {
		return clubId;
	}

	public void setClubId(Integer clubId) {
		this.clubId = clubId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getScoreType() {
		return scoreType;
	}

	public void setScoreType(Integer scoreType) {
		this.scoreType = scoreType;
	}

	public Integer getPlayerNum() {
		return playerNum;
	}

	public void setPlayerNum(Integer playerNum) {
		this.playerNum = playerNum;
	}

	public Integer getSanQiSuanJia() {
		return sanQiSuanJia;
	}

	public void setSanQiSuanJia(Integer sanQiSuanJia) {
		this.sanQiSuanJia = sanQiSuanJia;
	}

	public Integer getDanDiaoSuanJia() {
		return danDiaoSuanJia;
	}

	public void setDanDiaoSuanJia(Integer danDiaoSuanJia) {
		this.danDiaoSuanJia = danDiaoSuanJia;
	}

	public Integer getGangScore() {
		return gangScore;
	}

	public void setGangScore(Integer gangScore) {
		this.gangScore = gangScore;
	}

	public Integer getHongZhongBao() {
		return hongZhongBao;
	}

	public void setHongZhongBao(Integer hongZhongBao) {
		this.hongZhongBao = hongZhongBao;
	}

	public Integer getGuaDaFeng() {
		return guaDaFeng;
	}

	public void setGuaDaFeng(Integer guaDaFeng) {
		this.guaDaFeng = guaDaFeng;
	}

	public Integer getBuJiaBuHu() {
		return buJiaBuHu;
	}

	public void setBuJiaBuHu(Integer buJiaBuHu) {
		this.buJiaBuHu = buJiaBuHu;
	}

	public Integer getPlayType() {
		return playType;
	}

	public void setPlayType(Integer playType) {
		this.playType = playType;
	}

	public Integer getQiangGangHu() {
		return qiangGangHu;
	}

	public void setQiangGangHu(Integer qiangGangHu) {
		this.qiangGangHu = qiangGangHu;
	}
	
	

}
