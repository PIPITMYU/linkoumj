package com.yzt.logic.util.GameUtil;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.BackFileUtil;
import com.yzt.logic.util.Cnst;
import com.yzt.logic.util.MahjongUtils;
import com.yzt.logic.util.RoomUtil;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 玩家分的统计
 * 
 * @author wsw_007
 *
 */
public class JieSuan {
	
	public static void xiaoJieSuan(String roomId) {
		RoomResp room = RedisUtil.getRoomRespByRoomId(roomId);
		List<Player> players = RedisUtil.getPlayerList(room);
		//需要做以下统计
		//以及大结算校验  这里会写小结算文件 并对房间进行初始化 
		boolean ziMo = false;//赢家是否自摸
		for (Player other : players) {
			if(other.getIsZiMo()){
				ziMo = true;
				break;
			}
		}
		//杠分单算,先取到每个玩家的杠分.
		for (Player player : players) {
			List<Action> actionList = player.getActionList();
			if (actionList != null && actionList.size() != 0) {
				for (Action action : actionList) {
					if (action.getType() == Cnst.ACTION_TYPE_DIANGANG || action.getType() == Cnst.ACTION_TYPE_PENGGANG) { // 明杠1分
						changeGangFen(action,players,player, room,1);
					} else if (action.getType() == Cnst.ACTION_TYPE_ANGANG) { // 暗杠2分
						changeGangFen(action,players,player, room,2);
					}
				}
			}
		}


		//FIXME
		//统计玩家各项数据 庄次数 胡的次数 特殊胡的次数 自摸次数 点炮次数 胡牌类型 具体番数 各个分数统计 
		if(room.getHuangZhuang() != null && room.getHuangZhuang() == true){
			//荒庄不荒杠
			for(Player p : players){
				p.setScore(p.getScore()+p.getGangScore());
			}
		}else{ //正常结算
			int fen = MahjongUtils.checkHuFenInfo(players,room); // 检查胡牌玩家的分数	
			int fanShu = getFanShu(players);
			
			// 计分方式：点炮包三家，听牌点炮三家付			
			if(room.getScoreType() == Cnst.BAOSANJIA) { //包三家
				if (ziMo) {//自摸
					sanJiaFu(players, fen,room,fanShu);
				}else{//点炮
					baoSanJia(players, fen,room,fanShu);
				}
			} else if(room.getScoreType() == Cnst.TINGSANJIAFU){ //听牌之后，点炮三家付
				if (ziMo) {
					sanJiaFu(players, fen,room,fanShu);
				}else{//点炮
					for(Player ps:players){
						if (ps.getIsDian()) {//点炮人
							if (ps.getTing()!=null) {//听牌
								sanJiaFu(players, fen,room,fanShu);
							}else{//没听牌
								baoSanJia(players, fen,room,fanShu);
							}
							break;
						}
					}
				}
			}		
		
			
			if(room.getWinPlayerId().equals(room.getZhuangId())){
				//庄不变
			}else{
				//下个人坐庄
				int index = -1;
				Long[] playIds = room.getPlayerIds();
				for(int i=0;i<playIds.length;i++){
					if(playIds[i].equals(room.getZhuangId())){
						index = i+1;
						if(index == playIds.length){
							index = 0;
						}
						break;
					}
				}
				room.setZhuangId(playIds[index]);
				room.setCircleWind(index+1);
				
				//不是第一局,并且圈风是东风 ,证明是下一圈了.
				if(room.getXiaoJuNum() != 1 && room.getCircleWind() == Cnst.WIND_EAST){
					room.setTotolCircleNum(room.getTotolCircleNum() == null ? 1:room.getTotolCircleNum()+1);
					room.setLastNum(room.getCircleNum() - room.getTotolCircleNum());
				}
			}
		}
		
	
		// 更新redis
		RedisUtil.setPlayersList(players);
		
		// 添加小结算信息
		List<Integer> xiaoJS = new ArrayList<Integer>();
		for (Player p : players) {
			xiaoJS.add(p.getThisScore()+p.getGangScore());
		}
		room.addXiaoJuInfo(xiaoJS);
		// 初始化房间
		room.initRoom();
		RedisUtil.updateRedisData(room, null);
		// 写入文件
		List<Map<String, Object>> userInfos = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", p.getUserId());
			map.put("gangScore", p.getGangScore());
			map.put("huScore", p.getThisScore());
			map.put("pais", p.getCurrentMjList());
			if(p.getIsHu()){
				map.put("isWin", 1);
				map.put("winInfo", p.getFanShu());
			}else{
				map.put("isWin", 0);
			}
			if(p.getIsDian()){
				map.put("isDian", 1);
			}else{
				map.put("isDian", 0);
			}
			if(p.getActionList() != null && p.getActionList().size() > 0){
				List<Object> actionList = new ArrayList<Object>();
				for(Action action : p.getActionList()){
					if(action.getType() == Cnst.ACTION_TYPE_CHI){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", action.getActionId());
						actionMap.put("extra", action.getExtra());
						actionList.add(actionMap);
						
					}else if(action.getType() == Cnst.ACTION_TYPE_ANGANG){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", -2);
						actionMap.put("extra", action.getActionId());
						actionList.add(actionMap);
					}else{
						actionList.add(action.getActionId());
					}
				}
				map.put("actionList", actionList);
			}			
			userInfos.add(map);
		}
		JSONObject info = new JSONObject();
		info.put("lastNum", room.getLastNum());
		info.put("baoPai", room.getBaoPai());
		info.put("userInfo", userInfos);
		BackFileUtil.save(100102, room, null, info,null);
		// 小结算 存入一次回放
		BackFileUtil.write(room);

		// 大结算判定 (玩的圈数等于选择的圈数)
		if (room.getTotolCircleNum() == room.getCircleNum()) {
			// 最后一局 大结算
			room = RedisUtil.getRoomRespByRoomId(roomId);
			room.setState(Cnst.ROOM_STATE_YJS);
			RedisUtil.updateRedisData(room, null);
			// 这里更新数据库吧
			RoomUtil.updateDatabasePlayRecord(room);
		}
	}
	
	private static Integer getFanShu(List<Player> players){
		int fanShu = 1;
		for(Player p:players){
			if (p.getIsHu()) {
				List<Integer> fans = p.getFanShu();
				for(Integer fan:fans){
					if (fan>50) {
						fanShu*=Cnst.HU_TYPE_SOCRE.get(fan);
					}
				}
				break;
			}
		}
		return fanShu;
	}
	
	/**
	 * 
	 * @Title: sanJiaFu   
	 * @Description: 三家付，包括点炮的三家付和自摸的三家付
	 * @param: @param players
	 * @param: @param fen
	 * @param: @param room      
	 * @return: void      
	 * @throws
	 */
	private static void sanJiaFu(List<Player> players,Integer fen,RoomResp room,Integer fanShu){
		int playerNum = room.getPlayerNum()-1;
		for(Player ps:players){
			if(ps.getIsHu()){//胡牌人
				if (ps.getIsZiMo()) {//自摸
					if (ps.getUserId().equals(room.getZhuangId())) {//庄家自摸
						ps.setThisScore(fen*fanShu*playerNum);
					}else{//非庄家自摸
						ps.setThisScore(fen*fanShu*(playerNum-1)+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_ZHUANG))*fanShu);//庄家多出
					}
				}else{//点炮
					if (ps.getUserId().equals(room.getZhuangId())) {//如果胡牌人是庄
						ps.setThisScore(fen*fanShu*(playerNum-1)+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO))*fanShu);//点炮人多出分
					}else{//胡牌人不是庄
						//需要看点炮人是不是庄
						for(Player pt:players){
							if (pt.getIsDian()) {
								if (pt.getUserId().equals(room.getZhuangId())) {//点炮人是庄
									ps.setThisScore(fen*fanShu*(playerNum-1)+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO)+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_ZHUANG))*fanShu);//点炮人自己是庄更多出分
								}else{//点炮人不是庄
									ps.setThisScore(fen*fanShu*(playerNum-2)+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO))*2*fanShu);//点炮人+庄多出分
								}
								break;
							}
						}
					}
				}
				ps.setScore(ps.getScore()+ps.getThisScore());
			}else{
				if (ps.getUserId().equals(room.getZhuangId())) {//如果是庄，多出分
					if (ps.getIsDian()) {//庄家点炮
						ps.setThisScore((fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO)+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_ZHUANG))*fanShu*-1);//点炮人自己是庄更多出分
					}else{//闲家点炮
						ps.setThisScore((fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_ZHUANG))*fanShu*-1);//庄更多出分
					}
				}else{//不是庄
					if (ps.getIsDian()) {//非庄家点炮
						ps.setThisScore((fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO))*fanShu*-1);//点炮人多出分
					}else{//没有点炮
						ps.setThisScore(fen*fanShu*-1);//
					}
				}
				ps.setScore(ps.getScore()+ps.getThisScore());
			}
		}
	}
	
	/**
	 * 
	 * @Title: baoSanJia   
	 * @Description: 包三家  
	 * @param: @param players
	 * @param: @param fen
	 * @param: @param room      
	 * @return: void      
	 * @throws
	 */
	private static void baoSanJia(List<Player> players,Integer fen,RoomResp room,Integer fanShu){
		int playerNum = room.getPlayerNum()-1;
		int dianPaoRenScore = 0;
		for(Player ps:players){
			if (ps.getIsDian()) {//点炮人
				if (ps.getUserId().equals(room.getZhuangId())) {//点炮人是庄
					ps.setThisScore(fen*fanShu*(playerNum-1)*-1+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO)+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_ZHUANG))*fanShu*-1);//点炮人多出分
				}else{//点炮人不是庄
					for(Player pt:players){
						if (pt.getIsHu()) {
							if (pt.getUserId().equals(room.getZhuangId())) {//胡牌人是庄
								ps.setThisScore(fen*fanShu*(playerNum-1)*-1+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO))*fanShu*-1);//点炮人多出分
							}else{//胡牌人不是庄
								ps.setThisScore(fen*fanShu*(playerNum-2)*-1+(fen+Cnst.HU_TYPE_SOCRE.get(Cnst.HU_TYPE_DIANPAO))*fanShu*2*-1);//点炮人+庄多出分
							}
							break;
						}
					}
				}
				dianPaoRenScore = ps.getThisScore()*-1;
				ps.setScore(ps.getScore()+ps.getThisScore());
			}
		}
		for(Player ps:players){
			if (ps.getIsHu()) {//胡牌人
				ps.setThisScore(dianPaoRenScore);//就是点炮人的输的分
				ps.setScore(ps.getScore()+ps.getThisScore());
				break;
			}
		}
	}

	public static void changeGangFen(Action action,List<Player> players, Player player,RoomResp room,Integer type) {
		Integer fen = room.getGangScore();
		//暗杠
		if(type == 2){//分数翻倍
			fen = fen * 2;
			
		}
		player.setGangScore(player.getGangScore()+fen*(room.getPlayerNum()-1));
		for(Player p:players){
			if(!p.getUserId().equals(player.getUserId())){
				p.setGangScore(p.getGangScore() - fen);
			}
		}
	}
	
}
