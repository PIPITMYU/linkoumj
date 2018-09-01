package com.yzt.logic.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.JudegHu.checkHu.Hulib;
import com.yzt.logic.util.JudegHu.checkHu.TableMgr;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 
 * @author wsw_007
 *
 */
public class MahjongUtils {

	static {
		// 加载胡的可能
		TableMgr.getInstance().load();
	}
	
	public static void main(String[] args) {
		// 设置玩家手牌信息
		 List<Integer> currentMjList = new ArrayList<Integer>();
		 currentMjList.add(1);
		 currentMjList.add(2);
		 currentMjList.add(6);
		 currentMjList.add(7);
		 currentMjList.add(8);
		 currentMjList.add(11);
		 currentMjList.add(12);
		 currentMjList.add(13);
		 currentMjList.add(10);
//		 设置玩家信息
		 Player player = new Player();
		 player.setIsZiMo(true);
		 player.setCurrentMjList(currentMjList);
		 List<Action> actionList = new ArrayList<Action>();
		 Action a1=new Action();
		 a1.setType(2);
		 a1.setExtra(3);
		 actionList.add(a1);
		 player.setActionList(actionList);
		 // 设置房间信息
		 RoomResp room = new RoomResp();
		 room.setBaoPai(10);
		 room.setBuJiaBuHu(1);
		 room.setGuaDaFeng(1);
		 room.setSanQiSuanJia(1);
		 room.setDanDiaoSuanJia(1);
		 room.setHongZhongBao(1);
		 List<Integer> checkHuInfo = checkHuInfo(player, room);
		 for (Integer integer : checkHuInfo) {
			if(integer==1){
				System.out.print("平胡,");	
			}else if(integer==2){
				System.out.print("夹胡-边,");	
			}else if(integer==3){
				System.out.print("夹胡-吊,");	
			}else if(integer==4){
				System.out.print("夹胡-夹,");	
			}else if(integer==5){
				System.out.print("夹胡-无1胡1,");	
			}else if(integer==7){
				System.out.print("自摸,");	
			}else if(integer==51){
				System.out.print("大扣,");	
			}else if(integer==52){
				System.out.print("摸宝,");	
			}else if(integer==53){
				System.out.print("刮大风,");	
			}else if(integer==54){
				System.out.print("宝中宝,");	
			}else if(integer==55){
				System.out.print("边宝,");	
			}else if(integer==56){
				System.out.print("漏宝,");	
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public static List<Integer> getPais(RoomResp room) {
		// 1-9万 ,10-18饼,19-27条,32红中.
		ArrayList<Integer> pais = new ArrayList<Integer>();
		for (int j = 0; j < 4; j++) {
			for (int i = 1; i <= 27; i++) {
				pais.add(i);
			}
			pais.add(32);
		}
		// 2.洗牌
		Collections.shuffle(pais);
		return pais;
		
	}

	
	/**
	 * 删除用户指定的一张牌
	 * 
	 * @param currentPlayer
	 * @return
	 */
	public static void removePai(Player currentPlayer, Integer action) {
		Iterator<Integer> pai = currentPlayer.getCurrentMjList().iterator();
		while (pai.hasNext()) {
			Integer item = pai.next();
			if (item.equals(action)) {
				pai.remove();
				break;
			}
		}
	}

	
	/**
	 * 
	 * @param room
	 *            房间
	 * @param currentPlayer
	 *            当前操作的玩家
	 * @return 返回需要通知的操作的玩家ID
	 */
	public static Long nextActionUserId(RoomResp room, Long lastUserId) {
		Long[] playerIds = room.getPlayerIds();

		for (int i = 0; i < playerIds.length; i++) {
			if (lastUserId == playerIds[i]) {
				if (i == playerIds.length - 1) { // 如果是最后 一个,则取第一个.
					return playerIds[0];
				} else {
					return playerIds[i + 1];
				}
			}
		}
		return -100l;
	}
	
	/**
	 * 
	 * @Title: getPais   
	 * @Description: 根据pai与34的对比，是否是
	 * @param: @param oldPais
	 * @param: @param pai
	 * @param: @return  返回是否换牌了
	 * @return: List<Integer>      
	 * @throws
	 */
	private static boolean getNewPaisForCheckHuRule(List<Integer> oldPais,Integer pai,RoomResp room,List<Player> players,Player p){ 
		if (pai!=34) {//自己摸的,如果摸到的是宝，需要把最后一张变成混
			if (room.getPlayType().equals(Cnst.PLAY_TYPE_DAIBAO)) {//房间带宝
				if (pai.equals(room.getBaoPai())||
						(room.getHongZhongBao().equals(Cnst.YES)&&pai==32)||
						(room.getGuaDaFeng().equals(Cnst.YES)&&checkGuaDaFeng(p,oldPais, pai))) {//摸到红中宝或者宝牌,//刮大风
					oldPais.set(oldPais.size()-1, 34);
					pai = 34;
					return true;
				}
			}
		}
		return false;
	} 

	/**
	 * 
	 * @Title: checkHuRule   
	 * @Description: 如果是自己摸的，玩家牌数必须是3n+2,别人打的，就是3n+1
	 * @param: @param p
	 * @param: @param room
	 * @param: @param pai
	 * @param: @param type
	 * @param: @param players
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean checkHuRule(Player p,RoomResp room ,Integer paiOne,Integer type,List<Player> players) {
		Integer tempPai = new Integer(paiOne);    
		boolean access = false;
		List<Integer> newList = getNewList(p.getCurrentMjList());
		//1、如果pai传的是34，说明调用来源是checkTing相关
		//2、如果不是34，说明是摸牌那里调用
		if (type.equals(Cnst.CHECK_TYPE_ZIJIMO)||type.equals(Cnst.CHECK_TYPE_HAIDIANPAI)) {
			if(getNewPaisForCheckHuRule(newList, tempPai, room,players,p)){//如果摸到宝牌，换成混
				tempPai = 34;
			}
		}else{//toEdit
			newList.add(tempPai); //检测要带上别人打出的牌) 
		}
		access = isAcess(p, newList);
		if (access) {
			if (room.getPlayType().equals(Cnst.PLAY_TYPE_DAIBAO)&&room.getBuJiaBuHu().equals(Cnst.YES)) {//不夹不胡
				for(Integer tp:newList){//不夹不胡的手牌必须是3n+1，要把最后一张移除
					if (tp.equals(tempPai)) {
						newList.remove(tp);
						break;
					}
				}
				if (isJia(newList, tempPai, room, p)) {
					access = true;
				}else{
					access = false;
				}
			}else{
				int[] pais = getCheckHuPai(newList, null);
				if (tempPai==34) {
					if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
						access = true;
					}else{
						access = false;
					}
				}else{
					if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
						access = true;
					}else{
						access = false;
					}
				}
			}
		}
		return access;		
	}
	
	/**
	 * 
	 * @Title: isAcess   
	 * @Description: 牌必须是3n+2的数量 
	 * @param: @param p
	 * @param: @param newList
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	private static boolean isAcess(Player p,List<Integer> newList){
		boolean access = false;
		if (checkYiJiu(p, newList)) {//幺九
			access = true;
		}
		if (access&&isKePai(p, newList)) {//刻
			access = true;
		}else{
			access = false;
		}
		if(access&&checkHasShun(p, newList)){//顺
			access = true;
		}else{
			access = false;
		}
		if (access&&!isQingYiSe(p, newList)) {//非清一色
			access = true;
		}else{
			access = false;
		}
		return access;
	}
 

	public static boolean checkHuRuleOOOOOOOOOOOOOOOOOOOOOOOOld(Player p,RoomResp room ,Integer pai,Integer type,List<Player> players) {
		List<Integer> newList = getNewList(p.getCurrentMjList());
//		if (!checkYiJiu(p, newList)||!isKePai(p, newList)) {
//			return false;
//		}
		if (type.equals(Cnst.CHECK_TYPE_ZIJIMO)||type.equals(Cnst.CHECK_TYPE_HAIDIANPAI)) {
//			if (room.getPlayType().equals(Cnst.PLAY_TYPE_DAIBAO)) {//带宝玩儿法
//				if (pai==room.getBaoPai()||room.getHongZhongBao().equals(Cnst.YES)&&pai==32||checkGuaDaFeng(players, pai)) {
//
//					if (room.getBuJiaBuHu()!=null&&room.getBuJiaBuHu().equals(Cnst.YES)) {
//						List<Integer> tempPais = getNewList(p.getCurrentMjList());
//						Integer tempPai = tempPais.remove(tempPais.size()-1);
//						
//						if (isJia(tempPais, tempPai,room,p)) {
//							return true;
//						}else{
//							return false;
//						}
//					}
//					
//					return false;
//				}
//			}
//			int[] pais = getCheckHuPai(newList, null);
//			if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
//				return true;
//			}
		}else{
			newList.add(pai); //检测要带上别人打出的牌) 
			if (room.getBuJiaBuHu()!=null&&room.getBuJiaBuHu().equals(Cnst.YES)) {
				List<Integer> tempPais = getNewList(p.getCurrentMjList());
				if (isJia(tempPais, pai,room,p)) {
					return true;
				}else{
					return false;
				}
			}
			int[] pais = getCheckHuPai(newList, null);
			if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
				return true;
			}
		}
		return false;		
	}
	
	
	
	private static boolean checkGuaDaFeng(Player p,List<Integer> pais,Integer pai){
		
		List<Action> list = p.getActionList();
		if (list!=null&&list.size()>0) {
			for(Action act:list){
				Integer action = act.getActionId();
				if (((action >=57 && action <=90)||(action >=197 && action <=230))&&(action-56==pai||action-56-140==pai)) {
					return true;
				}
			}
		}
		
		//此时的pais是3n+2的数量
		int paiNum = Collections.frequency(pais, pai);
		if (paiNum==4) {
			int[] paisArray = getCheckHuPai(pais, null);
			paisArray[pai-1]-=3;
			if (Hulib.getInstance().get_hu_info(paisArray, 34, pai-1)) {//剩下的一张牌，得变成混牌
				return true;
			}
			
		}
		
		return false;
	}
	
	
	
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean checkTingOOOOOOOOOOld(Player p,RoomResp room){
		List<Integer> newList = getNewList(p.getCurrentMjList());
		newList.add(34);//加一个混
		int[] pais = getCheckHuPai(newList, null);
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33) {  
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
					List<Integer> list = getListFromArray(pais);
					if ((pais[31]>=1||(checkYiJiu(p, list)&&isKePai(p, list)))&&checkHasShun(p, list)&&!isQingYiSe(p,list)) {//有刻、有19、非清一色
						if (room.getBuJiaBuHu()!=null&&room.getBuJiaBuHu().equals(Cnst.YES)) {
							for(Integer pp : list){
								if (pp==34) {
									list.remove(pp);
									break;
								}
							}
							if (isJia(list, 34,room,p)) {
								return true;
							}else{
								continue;
							}
						}
						return true;
					}
				}
				pais[n]++;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean checkTing(Player p,RoomResp room,List<Player> players){
		boolean ting = false;
		List<Integer> temp = p.getCurrentMjList();
		List<Integer> newList = getNewList(temp);
		newList.add(34);//加一个混
		int[] pais = getCheckHuPai(newList, null);
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33) {  
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
					List<Integer> list = getListFromArray(pais);
					p.setCurrentMjList(list);
					if (checkHuRule(p, room, 34, Cnst.CHECK_TYPE_ZIJIMO, players)) {//有刻、有19、非清一色
						ting  = true;
						break;
					}
				}
				pais[n]++;
			}
		}
		p.setCurrentMjList(temp);
		return ting;
	}
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static List<Integer> checkTingToChuList(Player p,RoomResp room,List<Player> players){
		List<Integer> tingToChuList = new ArrayList<Integer>();
		List<Integer> temp = p.getCurrentMjList();
		List<Integer> newList = getNewList(temp);
		newList.add(34);//加一个混   
		int[] pais = getCheckHuPai(newList, null); 
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33) {
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
					List<Integer> list = getListFromArray(pais);
					p.setCurrentMjList(list);
					if (checkHuRule(p, room, 34, Cnst.CHECK_TYPE_ZIJIMO, players)) {//有刻、有19、非清一色
						tingToChuList.add(n+1);
					}
				}
				pais[n]++;
			}
		}
		if (tingToChuList.size()>0) {
			p.setTingToChuList(tingToChuList);
		}
		p.setCurrentMjList(temp);
		return tingToChuList.size()>0?tingToChuList:null;
	}
	
	
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static List<Integer> checkTingToChuListOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOld(Player p,RoomResp room){
		List<Integer> tingToChuList = new ArrayList<Integer>();
		List<Integer> newList = getNewList(p.getCurrentMjList());
		newList.add(34);//加一个混   
		int[] pais = getCheckHuPai(newList, null); 
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33 ) {
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
					List<Integer> list = getListFromArray(pais);
					if ((pais[31]>=1||(checkYiJiu(p, list)&&isKePai(p, list)))&&checkHasShun(p, list)&&!isQingYiSe(p,list)) {//有刻、有19、非清一色
						
						if (room.getBuJiaBuHu()!=null&&room.getBuJiaBuHu().equals(Cnst.YES)) {
							for(Integer pp : list){
								if (pp==34) {
									list.remove(pp);
									break;
								}
							}
							if (isJia(list, 34,room,p)) {
								tingToChuList.add(n+1);
							}
							pais[n]++;
							continue;
						}
						tingToChuList.add(n+1);
					}
				}
				pais[n]++;
			}
		}
		if (tingToChuList.size()>0) {
			p.setTingToChuList(tingToChuList);
		}
		return tingToChuList.size()>0?tingToChuList:null;
	}
	
	/**
	 * @Description: 夹胡的所有检测，包括1、正常夹胡；2、单吊是否算夹；3、37是否算夹；，这里的player只用到actionList了，不会用手牌
	 * @param: @param pais 不包含pai,必须是3n+1的张数
	 * @param: @param pai 单独的那张牌
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	
	private static boolean isJia(List<Integer> pais,Integer pai,RoomResp room,Player p){
		boolean isJia = false; 
		
		int[] paisArray = getCheckHuPai(pais, null);
		int num = 0;
		for(int i=1;i<=32;i++){
			paisArray[i-1]++;
			if (Hulib.getInstance().get_hu_info(paisArray, 34, 34)) {
				if (isAcess(p, getListFromArray(paisArray))) {
					num++;
					if (num>1) {
						break;
					}
				}
			}
			paisArray[i-1]--;
		}
		
		if (num!=1) {
			return isJia;
		}
		
		
		//正常的夹胡，夹中间
		int hunNum = 0;
		Integer hunPai = 34;
		if (pai==34) {
			hunNum = 0;  
			hunPai = pai;
		}
		isJia = BestBasicMJCheckUtil.checkKa(getCheckHuPai(pais, null), hunNum, hunPai, pai);
		
		
		if (!isJia) {//不是正常的夹中间
			
			//单吊夹
			if (room.getDanDiaoSuanJia().equals(Cnst.YES)) {//单吊算夹，检测单吊
				isJia = BestBasicMJCheckUtil.checkDiao(getCheckHuPai(pais, null), hunNum, hunPai, pai);
			}
		}
		
		if (!isJia) {//不是正常的夹中间
			//37边夹
			if (room.getSanQiSuanJia().equals(Cnst.YES)) {//37边夹
				isJia = BestBasicMJCheckUtil.checkBian(getCheckHuPai(pais, null), hunNum, hunPai, pai);
			}
		}
		
		if (!isJia) {
			Integer nousePai = 2;//先加一个不是混的不是19的牌，看看是否有幺九
			pais.add(nousePai);
			if (!checkYiJiu(p, pais)) {//如果没有幺九的话，进来这个条件，就是能胡的情况
				if (pai==34||pai==1||pai==9||pai==10||pai==18||pai==19||pai==27) {//正常19牌，34是混
					isJia = true;
				}else{//看是不是宝牌
					if (room.getPlayType().equals(Cnst.PLAY_TYPE_DAIBAO)&&room.getBaoPai().equals(pai)) {//是普通宝牌
						isJia = true;
					}else if(room.getPlayType().equals(Cnst.PLAY_TYPE_DAIBAO)&&room.getHongZhongBao().equals(Cnst.YES)&&pai==32){//是红中宝
						isJia = true;
					}
				}
			}
			pais.remove(nousePai);
		}
		return isJia;
	}
	
	/**
	 * 
	 * @Title: isQingYiSe   
	 * @Description: 重点想要不是清一色的结果
	 * @param: @param p
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean isQingYiSe(Player p,List<Integer> pais){
		boolean isQingYiSe = true;
		int type = 0;
		a:for(Integer pai:pais){
			if (pai!=32&&pai!=34) {
				type = (pai-1)/9;
				for(Integer temp:pais){
					if (temp!=32&&temp!=34&&(temp-1)/9!=type) {
						isQingYiSe = false;
						break a;
					}
				}
			}
		}
		if (isQingYiSe) {
			List<Action> actions = p.getActionList();
			if (actions!=null&&actions.size()>0) {
				for(Action act:actions){
					Integer actId = act.getActionId();
					if ((actId >=35 && actId <=56)||(actId >=175 && actId <=196)) {//吃或者抢吃
						if(actId >=175 && actId <=196){
							actId = actId-140;
						}
						int[] chis = Cnst.chiMap.get(actId);
						if ((chis[0]-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}else if((actId >=57 && actId <=90)||(actId >=197 && actId <=230)){
						if(actId >=197 && actId <=230){
							actId = actId-140;
						}
						if ((actId-56-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}else if(actId >=91 && actId <=126){
						if ((actId-90-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}
					
				}
			}
		}
		return isQingYiSe;
	}
	
	
	
	/**
	 * 
	 * @Title: checkHasShun   
	 * @Description: 牌数必须是3n+2，33位是混
	 * @param: @param p
	 * @param: @param shouPais
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	private static boolean checkHasShun(Player p,List<Integer> shouPais){
		//检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		//1吃   2碰  3点杠 4碰杠 5暗杠 
		for (Action action : actionList) {
			if(action.getType()==1){
				return true;
			}
		}
		int[] pais = getCheckHuPai(shouPais, null); 
		for (int n = 0; n < pais.length; n++) { 
			if(n<28&&pais[n]>=1){//正常手牌里的牌
				if (n>=0&&n<=6||n>=9&&n<=15||n>=18&&n<=24) {//一万到7万之间，一并到7并之间，一条到7条之间
					if (checkSHun(pais, n)) {
						return true;
					}
				}
				
			}else if(n<28&&pais[n]==0){
				if (n==6||n==15||n==24) {//一万到7万之间，一并到7并之间，一条到7条之间
					if (pais[n+1]>=1&&pais[n+2]>=1&&pais[33]>0) {
						pais[n+1]--;
						pais[n+2]--;
						pais[33] = 0;
						if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
							return true;
						}else {
							pais[n+1]++;
							pais[n+2]++;
						}
					}
				}
			}
		}
		return false;
	}
	
	
	private static boolean checkSHun(int[] pais,int n){
		boolean hasHun = pais[33]>0;
		boolean hasChi = false;
		if (hasHun) {//有混
			if (pais[n]>=1&&pais[n+1]>=1) {//正好胡顺子，混用在顺子上
				pais[n]--;
				pais[n+1]--;
				pais[33] = 0;
				if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
					hasChi = true;
				}else{
					pais[n]++;
					pais[n+1]++;
					pais[33] = 1;
					if (pais[n]>=1&&pais[n+1]>=1&&pais[n+2]>=1) {//不胡顺子，混没用在顺子上
						pais[n]--;
						pais[n+1]--;
						pais[n+2]--;
						if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
							hasChi = true;
						}else{
							pais[n]++;
							pais[n+1]++;
							pais[n+2]++;
							pais[33] = 1;
						}
					}
				}
				
			}else if(pais[n]>=1&&pais[n+2]>=1){
				pais[n]--;
				pais[n+2]--;
				pais[33] = 0;
				if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
					hasChi = true;
				}
			}
		}else{//没有混
			if (pais[n]>=1&&pais[n+1]>=1&&pais[n+2]>=1) {
				pais[n]--;
				pais[n+1]--;
				pais[n+2]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
					hasChi = true;
				}else{
					pais[n]++;
					pais[n+1]++;
					pais[n+2]++;
				}
			}
		}
		
		return hasChi;
	}
	
	/***
	 * 检测七对
	 * @param p
	 * @param newList
	 * @param pai
	 * @return
	 */
	public static boolean checkQiDui(Player p,List<Integer> newList) {
		if(newList.size()!=14){
			return false;
		}
		Integer hunNum = 0;		
		int oneNum=0;
		int[] checkHuPai = getCheckHuPai(newList, null);
		for (int i : checkHuPai) {
			if(i==1 || i==3){
				oneNum++;
			}
		}
		if(oneNum<=hunNum){
			return true;
		}
		return false;
	}
	

	/**
	 * 检测动作集合
	 * @param p
	 * @param pai
	 * @param room
	 * @param type
	 * @param checkChi 自己打的牌true,不提示吃.
	 * @return
	 */
	public static List<Integer> checkActionList(Player p, Integer pai, RoomResp room,Integer type,Boolean checkChi,List<Player> players) {
		List<Integer> actionList = new ArrayList<Integer>();
		
		boolean ting = p.getTing()!=null;
		
		if (type == Cnst.CHECK_TYPE_ZIJIMO) {//自摸
			if (ting) {
				if (checkHuRule(p,room,pai,type,players)) {
					
					actionList.add(500);	
				}
			}else{
				if (checkTing(p, room,players)) {
					actionList.add(Cnst.ACTION_BIANMA_TING);//听
				}
				//自摸的时候,检测能不能碰杠
				if(Cnst.CHECK_TYPE_ZIJIMO == type){
					List<Integer> pengGang = checkPengGang(p, pai);
					if(pengGang.size() != 0){
						actionList.addAll(pengGang);
					}
					
				}
				//自摸的时候,检测能不能暗杠.
				if(isShouBaYi(p, type, Cnst.ACTION_TYPE_ANGANG) && Cnst.CHECK_TYPE_ZIJIMO == type ){
					List<Integer> checkAnGang = checkAnGang(p,room);
					for (int i = 0; i < checkAnGang.size(); i++) {
						actionList.add(checkAnGang.get(i));
					}
				}
				
			}
		}else if(type == Cnst.CHECK_TYPE_BIERENCHU){//别人出
			if (ting) {
				if (checkHuRule(p,room,pai,type,players)) {
					actionList.add(500);	
				}
			}else{
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_CHI) &&  type != Cnst.CHECK_TYPE_ZIJIMO  && checkChi(p, pai)) {//吃
					List<Integer> c = chi(p, pai);
					if (!checkChi) {//不让检测吃
						if (type.equals(Cnst.CHECK_TYPE_BIERENCHU)) {//别人出的时候，不让检测出时，要检测能否吃听
							c = checkChiTing(c, p,pai,room,players);
							if (c!=null) {
								actionList.addAll(c);
							}
						}
					}else{
						actionList.addAll(c);
						List<Integer> list = checkChiTing(c, p,pai,room,players);
						if (list!=null) {
							actionList.addAll(list);
						}
					}
					
				}
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_PENG) && type != Cnst.CHECK_TYPE_ZIJIMO  &&  checkPeng(p, pai)) {//碰
					Integer peng = peng(p, pai);
					actionList.add(peng);	
					//检测是否能碰听
					if (checkPengTing(peng, p, pai, room,players)) {
						actionList.add(peng+140);
					}
				}
				//不是自摸,检测别人出牌的时候,能不能点杠.
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_DIANGANG) && Cnst.CHECK_TYPE_ZIJIMO != type && checkGang(p, pai)) {
					Integer gang = gang(p, pai,false);
					actionList.add(gang);
				}
			}
		}else if(type == Cnst.CHECK_TYPE_QIANGGANG){//抢杠胡
			if (ting&&checkHuRule(p,room,pai,type,players)) {
				actionList.add(500);	
			}
		}else if(type == Cnst.CHECK_TYPE_HAIDIANPAI){//海底牌
			if (ting&&checkHuRule(p,room,pai,type,players)) {
				actionList.add(500);	
			}
		}
		
//先注释掉		
//		if (ting) {
//			if (checkHuRule(p,room,pai,type)) {
//				actionList.add(500);	
//				return actionList;							
//			}
//		}else{ 
//			if (type.equals(Cnst.CHECK_TYPE_ZIJIMO)&&checkTing(p, room)){//如果是自摸，没听的时候，也得检测听
//				actionList.add(Cnst.ACTION_BIANMA_TING);//听
//			}
//			if (isShouBaYi(p, type, Cnst.ACTION_TYPE_CHI) &&  type != Cnst.CHECK_TYPE_ZIJIMO  && checkChi(p, pai)) {//吃
//				List<Integer> c = chi(p, pai);
//				if (!checkChi) {//不让检测吃
//					if (type.equals(Cnst.CHECK_TYPE_BIERENCHU)) {//别人出的时候，不让检测出时，要检测能否吃听
//						c = checkChiTing(c, p,pai,room);
//						if (c!=null) {
//							actionList.addAll(c);
//						}
//					}
//				}else{
//					actionList.addAll(c);
//					List<Integer> list = checkChiTing(c, p,pai,room);
//					if (list!=null) {
//						actionList.addAll(list);
//					}
//				}
//				
//			}
//			
//			if (isShouBaYi(p, type, Cnst.ACTION_TYPE_PENG) && type != Cnst.CHECK_TYPE_ZIJIMO  &&  checkPeng(p, pai)) {
//				Integer peng = peng(p, pai);
//				actionList.add(peng);	
//				//检测是否能碰听
//				if (checkPengTing(peng, p, pai, room)) {
//					actionList.add(peng+140);
//				}
//				
//				
//			}	
//			//1,不是自摸,检测别人出牌的时候,能不能点杠.
//			if (isShouBaYi(p, type, Cnst.ACTION_TYPE_DIANGANG) && Cnst.CHECK_TYPE_ZIJIMO != type && checkGang(p, pai)) {
//				Integer gang = gang(p, pai,false);
//				actionList.add(gang);
//			}
//			//2,自摸的时候,检测能不能暗杠.
//			if(isShouBaYi(p, type, Cnst.ACTION_TYPE_ANGANG) && Cnst.CHECK_TYPE_ZIJIMO == type ){
//				List<Integer> checkAnGang = checkAnGang(p,room);
//				for (int i = 0; i < checkAnGang.size(); i++) {
//					actionList.add(checkAnGang.get(i));
//				}
//			}
//		}
//		
//		//3,自摸的时候,检测能不能碰杠
//		if(isShouBaYi(p, type, Cnst.ACTION_TYPE_PENGGANG) && Cnst.CHECK_TYPE_ZIJIMO == type){
//			List<Integer> pengGang = checkPengGang(p, pai);
//			if(pengGang.size() != 0){
//				actionList.addAll(pengGang);
//			}
//			
//		}
		
		if (actionList.size() != 0) {
			actionList.add(0);
		}else{
			//没有动作 只能出牌
			if(type == Cnst.CHECK_TYPE_ZIJIMO){
				actionList.add(501);
			}		
		}		
		return actionList;
	}
	
	private static boolean checkPengTing(Integer oldAction,Player p,Integer pai,RoomResp room,List<Player> players){
		boolean pengTing = false;
		List<Integer> newPais = getNewList(p.getCurrentMjList());
		int[] pais = getCheckHuPai(newPais, null);
		pais[pai-1]-=2;
		List<Integer> temp = getListFromArray(pais);

		Action act = new Action(2, oldAction, p.getUserId(), null, pai);
		p.getActionList().add(act);
		p.setCurrentMjList(temp);
		if (checkTing(p, room,players)) {
			pengTing = true;
		}
		p.getActionList().remove(act);
		p.setCurrentMjList(newPais);
		return pengTing;
	}
	
	private static List<Integer> checkChiTing(List<Integer> oldChiActions,Player p,Integer pai,RoomResp room,List<Player> players){
		List<Integer> list = new ArrayList<Integer>();
		List<Integer> newPais = getNewList(p.getCurrentMjList());
		//c中放的是行为编码
		for (int n = 0; n < oldChiActions.size(); n++) {
			int[] pais = getCheckHuPai(newPais, null);
			int[] chiDePais = Cnst.chiMap.get(oldChiActions.get(n));
			for (int m = 0; m < chiDePais.length; m++) {//chiDePais里面装的就是1-34
				if (chiDePais[m]!=pai) {
					pais[chiDePais[m]-1]--;
				}
			}
			List<Integer> temp = getListFromArray(pais);
			p.setCurrentMjList(temp);
			
			Action act = new Action(1, oldChiActions.get(n), p.getUserId(), null, pai);
			p.getActionList().add(act);
			if (checkTing(p, room,players)) {
				list.add(oldChiActions.get(n)+140);
			}
			p.getActionList().remove(act);
		}
		p.setCurrentMjList(newPais);
		
		return list.size()>0?list:null;
	}
	
	private static List<Integer> getListFromArray(int[] pais){
		List<Integer> list = new ArrayList<Integer>();
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]!=0) {
				for (int a = 0; a < pais[n]; a++) {
					list.add(n+1);
				}
			}
		}
		return list;
		
	}
	

	/**
	 * 检测能不能碰完以后再开杠.
	 * @param p
	 * @return
	 */
	private static List<Integer> checkPengGang(Player p, Integer pai) {
		List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
		List<Integer> newList = getNewList(p.getCurrentMjList());
		List<Integer> gangList = new ArrayList<Integer>();
		for (int i = 0; i < actionList.size(); i++) {
			if(actionList.get(i).getType() == 2){
				for(int m=0;m<newList.size();m++){
					if(newList.get(m) == actionList.get(i).getExtra()){
						gangList.add(newList.get(m)+90);
					}
				}
			}
		}
		return gangList;
	}
	
	/**
	 * 手把一
	 * @param p
	 * @param type
	 * @return
	 */
	public static boolean isShouBaYi(Player p,Integer type,Integer actionType){
		//飘可以手把一
//		List<Action> actions = p.getActionList();
//		in : if(actions!=null &&  actionType != Cnst.ACTION_TYPE_CHI && actions.size() == 3){
//			for(Action action:actions){
//				if(action.getType() == Cnst.ACTION_TYPE_CHI){
//					break in;
//				}
//			}
//			return true;
//		}
//		if(type == Cnst.CHECK_TYPE_BIERENCHU){
//			if(p.getCurrentMjList().size() <= 4){
//				return false;
//			}
//			return true;
//		}
//		if(type == Cnst.CHECK_TYPE_ZIJIMO || type == Cnst.CHECK_TYPE_HAIDIANPAI){
//			if(p.getCurrentMjList().size() <= 5){
//				return false;
//			}
//			return true;
//		}
		//无论什么情况，都不能手把一
		if(p.getCurrentMjList().size() <= 5){
			return false;
		}
		return true;
	}

	/**
	 * 牌型是否有刻牌(三个一样或者四个一样),传入的时候，牌数是3n+2，33位是混
	 * @param p
	 * @return true 是
	 */
	public static boolean isKePai(Player p,List<Integer> newList) {
		//检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		//1吃   2碰  3点杠 4碰杠 5暗杠 
		for (Action action : actionList) {
			if(action.getType()!=1){
				return true;
			}
		}
		//检测手中有没有刻
		Set<Integer> distinct=new HashSet<Integer>();
		for (Integer integer : newList) {
			if (integer == 32) {//如果手牌里有红中，就肯定有刻牌
				return  true;
			}
			distinct.add(integer);
		}
		//手牌中是否有3张,这3张移除必须能胡才行 比如12333
		int num=0;
		for (Integer distinctPai : distinct) {
			num = Collections.frequency(newList, distinctPai);
			if(num>=3){					
				int[] huPaiZu = getCheckHuPai(newList,null); 
				if (huPaiZu[33]>0) {//带混进来的
					//将这3张牌移除
					huPaiZu[distinctPai-1] = num - 3;
					if (Hulib.getInstance().get_hu_info(huPaiZu, 34, 33)) {
						return true;
					}
				}else{
					//将这3张牌移除
					huPaiZu[distinctPai-1] = num - 3;
					if (Hulib.getInstance().get_hu_info(huPaiZu, 34, 34)) {
						return true;
					}
				}
				
			}
		}
		for (Integer distinctPai : distinct) {
			num = Collections.frequency(newList, distinctPai);
			if(num>=2){					
				int[] huPaiZu = getCheckHuPai(newList,null); 
				if (huPaiZu[33]>0) {
					//将这2张牌移除
					huPaiZu[distinctPai-1] = num - 2;
					huPaiZu[33] = 0;//移除混
					if (Hulib.getInstance().get_hu_info(huPaiZu, 34, 34)) {
						return true;
					}
				}
				
			}
		}
		
		
		return false;
	}

	/**
	 * 胡牌必须有幺九.checkTing调用时，shouPai必须3n+2，混放在最后一位上
	 * @param p
	 * @param list
	 * @return
	 */
	private static boolean checkYiJiu(Player p, List<Integer> shouPai) {
		//检测手牌是否有1-9
		for (int i = 0; i < shouPai.size(); i++) {
			if (shouPai.get(i) == 1  || shouPai.get(i) == 10 || shouPai.get(i) == 19 || shouPai.get(i) == 9 || shouPai.get(i) == 18  || shouPai.get(i) == 27 || shouPai.get(i) == 32 ) {
					return true;	

			}
		}
		//判断有动作的牌类型是否相同
		List<Action> actionList = p.getActionList();
		if(actionList.size()>0){
			for (Action action : actionList) { 
				if(Cnst.ACTION_YI_JIU.contains(action.getActionId())){
					return true;
				}
			}
		}
		
		//这里是无一胡一  的幺九检测
		int[] pais = getCheckHuPai(shouPai, null);
		if (pais[33]>0) {//带混的，是checkTing传过来的
			pais[33]=0;//把混去掉，然后把1、9挨个往里添加，检测胡
			int n=0;
			while(n<27){
				pais[n]++;
				if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
					return true;
				}else{
					pais[n]--; 
					n+=9;
				}
			}
			n=8;
			while(n<27){
				pais[n]++;
				if (Hulib.getInstance().get_hu_info(pais, 34, 34)) {
					return true;
				}else{
					pais[n]--;
					n+=9;
				}
			}
		}
		return false;
		//检测手里有没有混
//		Integer hunNum = hunNum(p, hun);
//		if(hunNum == null || hunNum == 0){
//			return false;
//		}
//		List<Integer> newList = getNewList(list);
//		Iterator<Integer> it = newList.iterator();
//		while(it.hasNext()){
//			Integer x = it.next();
//			if(x == hun){
//				it.remove();
//			}
//		}
//		for (Integer i : Cnst.PAI_YI_JIU) {
//			newList.add(i);
//			int[] checkHu = getCheckHuPai(newList, null);
//			checkHu[33] = hunNum - 1;
//			if(Hulib.getInstance().get_hu_info(checkHu,34,33)){
//				//检测是碰碰胡 或 平胡
//				if(isPengPengHu(p, newList, hunNum-1)){
//					p.getFanShu().add(Cnst.YAOJIUPENGPENGHU);
//				}else{
//					p.getFanShu().add(Cnst.BUSHIYAOJIUPENGPENGHU);
//				}
//				return true;
//			}else{
//				it = newList.iterator();
//				a:while(it.hasNext()){
//					Integer x = it.next();
//					if(x == i){
//						it.remove();//!!!
//						break a;
//					}
//				}
//			}
//			
//		}
//		return false;
	}
	/***
	 * 根据出的牌 设置下个动作人和玩家
	 * @param players
	 * @param room
	 * @param pai
	 */
	public static void getNextAction(List<Player> players, RoomResp room, Integer pai){
		Integer maxAction = 0;
		Long nextActionUserId = -1L;
		List<Integer> nextAction = new ArrayList<Integer>();
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for(int i=0;i<playIds.length;i++){
			if(playIds[i].equals(room.getLastChuPaiUserId())){
				index = i+1;
				if(index == playIds.length){
					index = 0;
				}
				break;
			}
		}
		Long xiaYiJia = playIds[index];
		//从下一家开始检测 多胡的话 会按顺序来
		Player[] checkList = new Player[players.size()];
		for(int i=0;i<players.size();i++){
			if(i == index){
				checkList[0] = players.get(i);
			}
			if(i<index){
				checkList[checkList.length-(index-i)] = players.get(i);
			}
			if(i>index){
				checkList[i-index] = players.get(i);
			}
		}
		for(Player p:checkList){
			if(p!=null&&!room.getGuoUserIds().contains(p.getUserId())){
				//玩家没点击过 或者不是 出牌的人  吃只检测下个人
				List<Integer> checkActionList;
				if(p.getUserId().equals(xiaYiJia)){
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,true,players);
				}else{
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,false,players);
				}
				
				if(checkActionList.size() == 0){
					//玩家没动作 
					room.getGuoUserIds().add(p.getUserId());
				}else{
					Collections.sort(checkActionList);
					if(checkActionList.get(checkActionList.size()-1) > maxAction){
						nextActionUserId = p.getUserId();
						nextAction = checkActionList;
						maxAction = checkActionList.get(checkActionList.size()-1);
					}
				}
			}
		}
		//如果都没可执行动作 下一位玩家请求发牌
		if(maxAction == 0){
			nextAction.add(-1);
			room.setNextAction(nextAction);
			//取到上个出牌人的角标 下一位来发牌
			room.setNextActionUserId(xiaYiJia);
		}else{
			room.setNextAction(nextAction);
			room.setNextActionUserId(nextActionUserId);
		}
	
	}

	/**
	 * 检查玩家能不能碰
	 * 
	 * @param p
	 * @param Integer
	 *            peng 要碰的牌
	 * @return
	 */
	public static boolean checkPeng(Player p, Integer peng) {
		int num = 0;
		for (Integer i : p.getCurrentMjList()) {
			if(i == peng){
				num++;
			}
		}
		if (num >= 2) {
			return true;
		}
		return false;
	}

	/**
	 * //与吃的那个牌能组合的List
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> reChiList(Integer action ,Integer chi){
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		for (int i = 35; i <= 56; i++) {
			if(i == action ){
				int[] js = Cnst.chiMap.get(action);
				for (int j = 0; j < js.length; j++) {
					if(js[j] != chi){
						arrayList.add(js[j]);
					}
				}
			}
		}
		return arrayList; 
	}
	
	/**
	 * 执行动作吃!
	 * 返回原本手里的牌
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> chi(Player p, Integer chi) {
		List<Integer> shouPai = getNewList(p.getCurrentMjList());
		Set<Integer> set = new HashSet<Integer>();
		List<Integer> reList = new ArrayList<Integer>();
		boolean a = false; // x<x+1<x+2
		boolean b = false; // x-1<x<x+1
		boolean c = false; // x-2<x-1<x

		// 万
		if (chi < 10) { // 基数34
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}

			if (a && chi != 9 && chi != 8) {
				set.add(34 + chi);
			}
			if (b && chi != 9) {
				set.add(33 + chi);
			}
			if (c) {
				set.add(32 + chi);
			}

			// 饼
		} else if (chi >= 10 && chi <= 18) { // 基数32
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 18 && chi != 17) {
				set.add(32 + chi);
			}
			if (b && chi != 10 && chi != 18) {
				set.add(31 + chi);
			}
			if (c && chi != 10 && chi != 11) {
				set.add(30 + chi);
			}
			// 条
		} else if (chi >= 19 && chi <= 27) { // 基数30
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 26 && chi != 27) {
				set.add(30 + chi);
			}
			if (b && chi != 19 && chi != 27) {
				set.add(29 + chi);
			}
			if (c && chi != 19 && chi != 20) {
				set.add(28 + chi);
			}
		}
		reList.addAll(set);
		return reList;
	}
	/**
	 * 执行动作杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static Integer gang(Player p, Integer gang, Boolean pengGang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		if(pengGang){
			List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
			for (int i = 0; i < actionList.size(); i++) {
				if(actionList.get(i).getType() == 2 && actionList.get(i).getExtra() == gang){
					return 90 + gang;
				}
			}
		}

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				if (key == gang) {
					return 90 + gang;
				}
			}
		}

		return -100;
	}

	/**
	 * 执行动作碰
	 * 
	 * @param p
	 * @param peng
	 * @return 行为编码
	 */
	public static Integer peng(Player p, Integer peng) {
		return 56 + peng;
	}

	/**
	 *  * 检测玩家能不能吃.10 与19特殊处理
	 * @param p
	 * @param chi
	 * @param hunPai 不能吃
	 * @return
	 */
	public static boolean checkChi(Player p, Integer chi) {
		List<Integer> list = getNewList(p.getCurrentMjList());
		boolean isChi = false;
		List<Integer> arr = new ArrayList<Integer>();
		arr.add(chi + 1);
		arr.add(chi + 2);
		if (list.containsAll(arr)) {
			isChi = true;
		}
		List<Integer> arr1 = new ArrayList<Integer>();
		List<Integer> arr2 = new ArrayList<Integer>();
		if (chi != 10 && chi != 19) {
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (list.containsAll(arr1)) {
				isChi = true;
			}
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (list.containsAll(arr2)) {
				isChi = true;
			}
		}
		return isChi;
	}

	/**
	 * 执行暗杠
	 * 
	 * @param p
	 * @return 返回杠的牌
	 */
	public static Integer anGang(Player p) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		Integer gang = 0;
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				gang = key;
			}
		}

		Iterator<Integer> iter1 = p.getCurrentMjList().iterator();
		while (iter1.hasNext()) {
			Integer item = iter1.next();
			if (item == gang) {
				iter1.remove();
			}
		}
		return gang + 90;
	}

	/**
	 * 检查能不能暗杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static List<Integer> checkAnGang(Player p,RoomResp room) {
		List<Integer> anGangList = new ArrayList<Integer>();
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4 ) { // 控制有几个重复的
				 anGangList.add(key+90);
			}
		}
		return anGangList;
	}

	/**
	 * 检测玩家能不能杠
	 * 1,明杠,2暗杠,3 点杠
	 * @param p
	 * @return
	 */
	public static boolean checkGang(Player p, Integer gang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的;
				if (key == gang) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param mahjongs
	 *            房间内剩余麻将的组合
	 * @param num
	 *            发的张数
	 * @return
	 */
	public static List<Integer> faPai(List<Integer> mahjongs, Integer num) {
		if (mahjongs.size() == 8) {
			return null;
		}
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			result.add(mahjongs.get(i));
			mahjongs.remove(i);
		}
		return result;
	}
	
	
	
	/**
	 * 返回一个新的集合
	 * @param old
	 * @return
	 */
	public static List<Integer> getNewList(List<Integer> old) {
		List<Integer> newList = new ArrayList<Integer>();
		if (old != null && old.size() > 0) {
			for (Integer pai : old) {
				newList.add( pai );
			}
		}
		return newList;
	}

//	/**
//	 * 丹阳推到胡规则 返回的是分
//	 * 
//	 * @param players
//	 * @param room
//	 * @return
//	 */
//	public static int checkHuFenInfo(List<Player> players,RoomResp room) {
//		Player p = null;
//		//胡牌就是1分
//		int fen = 1;
//		List<Integer> winInfo = new ArrayList<Integer>();
//		for (Player player : players) {
//			if (player.getIsHu()) {
//				player.setHuNum(player.getHuNum() + 1);
//				p = player;
//			}
//		}		
//
//		if(room.getZhuangId().equals(p.getUserId())){
//			winInfo.add(Cnst.ZHUANG);
//		}
//		//清一色 
//		if(isQingYiSe(p, room, p.getCurrentMjList())){
//			winInfo.add(Cnst.QINGYISE);
//			fen = fen * 4;
//		}
//		List<Integer> newList = getNewList(p.getCurrentMjList());
//		//飘
//		if(isPengPengHu(p, newList, 0)){
//			winInfo.add(Cnst.PIAO);
//			fen = fen * 4;
//		}else{
//			//夹胡
//			if(checkKaBianDiao(p, room)){
//				winInfo.add(Cnst.JIAHU);
//				fen = fen * 2;
//				Integer huDePai = p.getCurrentMjList().get(p.getCurrentMjList().size()-1);
//			}
//		}
//		
//		if(!winInfo.contains(Cnst.JIAHU)&&!winInfo.contains(Cnst.QINGYISE)&&!winInfo.contains(Cnst.PIAO)&&!winInfo.contains(Cnst.QIXIAODUI)&&!winInfo.contains(Cnst.TE)){
//			winInfo.add(Cnst.PINGHU);
//		}
//		//门清两番
//		if(p.getActionList()!=null && p.getActionList().size() == 0){
//			fen = fen * 2;
//			winInfo.add(Cnst.MENQING);
//		}
//		//自摸两番
//		if(p.getIsZiMo()){
//			fen = fen * 2;
//			winInfo.add(Cnst.ZIMO);
//		}else{
//			winInfo.add(Cnst.DIANPAO);
//		}
//		p.setFanShu(winInfo);
//		
//		return fen;
//	}
	
	

	/**
	 * 从牌桌上,把玩家吃碰杠的牌移除.
	 * @param room
	 * @param players
	 */
	
	public static void removeCPG(RoomResp room, List<Player> players) {
		Player currentP = null;
		for (Player p : players) {
			if(p.getUserId().equals(room.getLastChuPaiUserId())){
				currentP = p;
				List<Integer> chuList = p.getChuList();
				Iterator<Integer> iterator = chuList.iterator();
				while(iterator.hasNext()){
					Integer pai = iterator.next();
					if(room.getLastChuPai() == pai ){
						iterator.remove();
						break;
					}
				}
			}
		}
		RedisUtil.updateRedisData(null, currentP);
	}
	
	/***
	 * 移除动作手牌 
	 * @param currentMjList
	 * @param chi
	 * @param action
	 * @param type
	 */
	public static void removeActionMj(List<Integer> currentMjList,List<Integer> chi,Integer action,Integer type){
		Iterator<Integer> it = currentMjList.iterator(); //遍历手牌,删除碰的牌
		switch (type) {
		case Cnst.ACTION_TYPE_CHI:
			int chi1 = 0;
			int chi2 = 0;
			a : while(it.hasNext()){
					Integer x = it.next();
					if(x == chi.get(0) && chi1 == 0){
						it.remove();
						chi1 = 1 ;
					}
					if(x == chi.get(1) && chi2 == 0){
						it.remove();
						chi2 = 1;
					}
					if(chi1 == 1 && chi2 == 1){
						break a;
					}
				}		
			break;
		case Cnst.ACTION_TYPE_PENG:
			int num = 0;
			while(it.hasNext()){
				Integer x = it.next();
			    if(x==action-56){
			        it.remove();
			        num = num + 1;
			        if(num == 2){
			        	break;
			        }
			    }
			}
			break;
		case Cnst.ACTION_TYPE_ANGANG:
			List<Integer> gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_PENGGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_DIANGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		default:
			break;
		}
	}
	/***
	 * 获得 检测胡牌的 34位数组 包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getCheckHuPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		List<Integer> newList = getNewList(currentList);
		if(pai!=null){
			newList.add(pai);
		}
		for(int i=0;i<newList.size();i++){
			int a = checkHuPai[newList.get(i) - 1];
			checkHuPai[newList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}

	/***
	 * 获得 检测胡牌的 34位数组 不包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getRemoveLastPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		Boolean hasRemove = false; 
		for(int i=0;i<currentList.size();i++){
			if(currentList.get(i) == pai && !hasRemove){
				hasRemove = true;
				continue;
			}
			int a = checkHuPai[currentList.get(i) - 1];
			checkHuPai[currentList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}
	
	/**
	 *  牌型是否是清一色 红中不算门 ,单门+红中胡牌算是清一色
	 * 
	 * @param p玩家
	 * @return
	 */
	public static boolean isQingYiSe(Player p , RoomResp room ,List<Integer> list) {
		Integer leixing=0;
		Boolean needcheck=false;
		List<Integer> newList = getNewList(list);
		Collections.sort(newList);
		Integer pai = newList.get(0);
		leixing=(pai-1)/9;
		if(leixing==3){//单调红中，只看吃碰杠类型就Ok
			needcheck=true;
		}else{//不是单调红中
			for (Integer shouPai : newList) {
				//红中跳出
				if((shouPai-1)/9==3){
					continue;
				}
				//要检测的类型不再相同
				if(leixing!=(shouPai-1)/9){
					return false;
				}
			}
		}
		//判断有动作的牌类型是否相同
		Integer extra=0;
		List<Action> actionList = p.getActionList();
		if(actionList.size()>0){
			if(needcheck){//绝对不会大于3了，因为红中只能碰一次
				leixing=(actionList.get(0).getExtra()-1)/9;
				needcheck=false;
			}
			for (Action action : actionList) {
				extra = action.getExtra();
				if(extra>=28){
					continue;
				}else{
					if(leixing!=(extra-1)/9){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * 牌型是不是碰碰胡(全是刻牌的牌型)
	 * 
	 * @param p
	 * @param hunNum 
	 * @param newList 
	 * @return
	 */
	public static boolean isPengPengHu(Player p, List<Integer> newList, Integer hunNum) {
		//检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		//1吃   2碰  3点杠 4碰杠 5暗杠 
		for (Action action : actionList) {
			if(action.getType()==1){
				return false;
			}
		}
		//检测手牌是不是都是刻
		int[] checkHuPai = getCheckHuPai(newList,null);
		int twoNum=0;
		int oneNum=0;
		for (Integer integer : checkHuPai) {
			if(integer==1){
				oneNum++;
			}else if(integer==2){
				twoNum++;
			}else if(integer==3){
			}else if(integer==4){
				return false;
			}
		}
		//两张的需要1张混，1张需要两张混,但是将减少一张混的需求
		if((twoNum+oneNum*2-1)<=hunNum){
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param winPlayer
	 * @param room
	 * @param isGuaDaFeng 
	 * @return  0不是夹胡   2边胡 3  吊胡      4卡胡 5无1胡1
	 */
	public static Integer checkKaBianDiao(Player winPlayer,RoomResp room) {
		List<Integer> currentMjList = winPlayer.getCurrentMjList();
		List<Integer> newList = getNewList(currentMjList);
		int size = newList.size();
		Integer dongZuoPai = newList.get(size-1);
		//移除这张牌剩下的牌集合
		newList.remove(size-1);
		//获取手牌的数组集合
		int[] shouPaiArr=new int[34];
		for (Integer integer : newList) {
			 int i = shouPaiArr[integer-1];
			 shouPaiArr[integer-1]=i+1;
		}
		//检测吊
		if(room.getDanDiaoSuanJia().equals(1)){
			if(shouPaiArr[dongZuoPai-1]!=0){
				int dongZuoNum = shouPaiArr[dongZuoPai-1];
				shouPaiArr[dongZuoPai-1]=dongZuoNum-1;
				if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
					return Cnst.HU_TYPE_JIAHU_DIAO;
				}
				//没胡,加回来
				shouPaiArr[dongZuoPai-1]=dongZuoNum;
			}
		}
		//最后那张不能是红中
		if(dongZuoPai!=32){
			//定义动作牌的数子大小
			int dongZuoPaiNum=dongZuoPai%9;
			//检测卡
			//动作牌不能是1和9 ----1,9怎么卡？
			if(dongZuoPaiNum!=1 && dongZuoPaiNum!=0){
				//看比它小1
				int smallOneNum = shouPaiArr[dongZuoPai-2];
				//看比它大1
				//看比它大1和小1的是否存在
				int bigOneNum = shouPaiArr[dongZuoPai];
				if(smallOneNum>0 &&  bigOneNum>0){
					shouPaiArr[dongZuoPai-2]=smallOneNum-1;
					shouPaiArr[dongZuoPai]=bigOneNum-1;
					if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
						return Cnst.HU_TYPE_JIAHU_JIA;
					}
					//没胡,加回来
					shouPaiArr[dongZuoPai-2]=smallOneNum;
					shouPaiArr[dongZuoPai]=bigOneNum;
				}
			}
			//检测边  --动作牌是3或者7
			if(room.getSanQiSuanJia().equals(1)){
				if(dongZuoPaiNum==3){
					//查看比它小2的
					int smallTwoNum = shouPaiArr[dongZuoPai-3];
					//查看比它小1的
					int smallOneNum = shouPaiArr[dongZuoPai-2];
					//看比它小2和小1的是否存在
					if(smallTwoNum>0 && smallOneNum>0){
						shouPaiArr[dongZuoPai-3]=smallTwoNum-1;
						shouPaiArr[dongZuoPai-2]=smallOneNum-1;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
							return Cnst.HU_TYPE_JIAHU_BIAN;
						}
						//没胡,加回来
						shouPaiArr[dongZuoPai-3]=smallTwoNum;
						shouPaiArr[dongZuoPai-2]=smallOneNum;
					}
				}else if(dongZuoPaiNum==7){
					//查看比它大1的
					int bigOneNum = shouPaiArr[dongZuoPai];
					//查看比它大2的
					int bigTwoNum = shouPaiArr[dongZuoPai+1];
					//看比它大1和大2的是否存在
					if(bigOneNum>0 && bigTwoNum>0){
						shouPaiArr[dongZuoPai]=bigOneNum-1;
						shouPaiArr[dongZuoPai+1]=bigTwoNum-1;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
							return Cnst.HU_TYPE_JIAHU_BIAN;
						}
						//没胡,加回来
						shouPaiArr[dongZuoPai]=bigOneNum;
						shouPaiArr[dongZuoPai+1]=bigTwoNum;
					}
				}
			}
			//检测特殊： 无一胡一或者无九胡九,如果胡 1,4 但是没1,9(1,9必须存在才胡),那么也算
			//有红中不用检测了,直接不成立
			if(shouPaiArr[32-1]>0){
				return 0;
			}
			//说明没有红中
			//动作牌必须是1或者9
			if(dongZuoPaiNum==1 || dongZuoPaiNum==0){
				//手牌里面必须没有1,9 和玩家的动作里面必须没有1和9
				if(!checkYiJiu(winPlayer, newList)){
					//手牌里面没有1,9
					return Cnst.HU_TYPE_JIAHU_WUYIHUYI;
				}
			}
		}
		return 0;
	}
	
	public static Integer checkHuFenInfo(List<Player> players, RoomResp room){
		int fen = 0;
		List<Integer> fens = null;
		
		for(Player p:players){
			if (p.getIsHu()) {
				fens = checkHuInfo(p, room);
				
				if (room.getZhuangId().equals(p.getUserId())) {
					fens.add(Cnst.HU_TYPE_ZHUANG);//庄
				}
				p.setFanShu(fens);
				break;
			}
		}
		
		for(Integer f:fens){
			if (f<50) {
				fen+=Cnst.HU_TYPE_SOCRE.get(f);
			}
		}
		return fen;
	}
	
	
	/**
	 * 自摸的才算宝分--摸宝,刮大风,宝中宝(宝吊宝)--只考虑红中宝和显示的宝,边宝(边),漏宝(卡)
	 * 别人出的都不是宝
	 * --摸宝--刮大风--宝中宝--边宝--漏宝--只显示一个
	 * @param players
	 * @param winPlayer2
	 * @param room
	 * @param type 
	 * @return
	 */
	public static List<Integer> checkHuInfo(Player winPlayer, RoomResp room) {
		// 获取胡的类型的集合
		List<Integer> huInfoList=new ArrayList<Integer>();
		//检测是不是大扣
		List<Action> actionList = winPlayer.getActionList();
		
		boolean daKou=true;
		if(actionList!=null && actionList.size()>0){
			for (Action action : actionList) {
				if(action.getType()!=Cnst.ACTION_TYPE_ANGANG){
					daKou=false;
					break;
				}
			}
		}
		if(daKou){
			huInfoList.add(Cnst.HU_TYPE_DAKOU);
		}
		//获取玩家手牌
		List<Integer> currentMjList = winPlayer.getCurrentMjList();
		List<Integer> newList = getNewList(currentMjList);
		int size = newList.size();
		Integer dongZuoPai = newList.get(size-1);
		//移除这张牌剩下的牌集合
		newList.remove(size-1);
		//获取手牌的数组集合
		int[] shouPaiArr=new int[34];
		for (Integer integer : newList) {
			 int i = shouPaiArr[integer-1];
			 shouPaiArr[integer-1]=i+1;
		}
		boolean isJia=true;
		int num = 0;
		for(int i=1;i<=32;i++){
			shouPaiArr[i-1]++;
			if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)) {
				if (isAcess(winPlayer, getListFromArray(shouPaiArr))) {
					num++;
					if (num>1) {
						shouPaiArr[i-1]--;
						break;
					}
				}
			}
			shouPaiArr[i-1]--;
		}
		
		if (num!=1) {
			isJia =false;
		}
		
		
		
		Boolean isBao=false;
		Boolean isGuaDaFeng=false;
		if(winPlayer.getIsZiMo()){//自摸
			huInfoList.add(Cnst.HU_TYPE_ZIMO);
			//先判断自摸的是不是宝
			Integer playType = room.getPlayType();
			boolean needCheck=false;
			if(playType.equals(Cnst.PLAY_TYPE_DAIBAO)){
				needCheck=true;
			}
			if(needCheck){
				Integer baoPai = room.getBaoPai();
//			needTingBaoList.add(baoPai);
				if(baoPai==dongZuoPai){
					isBao=true;
				}
				if(!isBao){
					if(room.getHongZhongBao().equals(Cnst.YES)){
						if(baoPai!=32){//开出的宝牌如果也是红中就不在检测了
//						needTingBaoList.add(32);
							if(dongZuoPai==32){
								isBao=true;
							}
						}
					}
				}
				if(!isBao){
					boolean checkGuaDafeng=true;
					if(room.getGuaDaFeng().equals(Cnst.YES)){
						//找到玩家手牌是否有碰
						if(actionList!=null && actionList.size()>0){
							for (Action action : actionList) {
								if(action.getType()==2){
									if(dongZuoPai==action.getExtra()){
										huInfoList.add(Cnst.HU_TYPE_GUADAFENG);
										isGuaDaFeng=true;
										checkGuaDafeng=false;
										break;
									}
								}
							}
						}
						//找到玩家手中是否有三张
						if(checkGuaDafeng){
							int x=shouPaiArr.length;
							for (int i = 0; i < x; i++) {
								if(shouPaiArr[i]==3){
									if(i+1==dongZuoPai){
										//这是摸的第四张--将这张设置为混排,那三张当刻被移除,此时混数量为1
										shouPaiArr[i]=1;
										if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, i)){
											//将这个刻移除，防止下面牌的判断
											shouPaiArr[i]=0;
											isGuaDaFeng=true;
											huInfoList.add(Cnst.HU_TYPE_GUADAFENG);
											break;
										}else{//不胡,说明这张牌不能当宝使用
											shouPaiArr[i]=3;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		//如果最后一张是宝牌
		if(isBao){//有宝
			Integer yiJiu=0;
			if(checkYiJiu(winPlayer, newList)){
				yiJiu=1;
			}
			Integer checkBianKaDiaoWithHun = checkBianKaDiaoWithHun(shouPaiArr,room,yiJiu);
			//获取胡的类型
			if(isJia && checkBianKaDiaoWithHun!=0){
				huInfoList.add(checkBianKaDiaoWithHun);
			}else{
				huInfoList.add(Cnst.HU_TYPE_PINGHU);
			}
			boolean needCheck=false;
			//获取是什么宝
			//获取原来收中宝这个位置的数量
			int z = shouPaiArr[dongZuoPai-1];
			//将宝当成普通牌放进去
			shouPaiArr[dongZuoPai-1]=z+1;
			if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
				shouPaiArr[dongZuoPai-1]=z;
				needCheck=true;
			}else{
				shouPaiArr[dongZuoPai-1]=z;
				if(dongZuoPai==32 && shouPaiArr[dongZuoPai-1]==3){
					huInfoList.add(Cnst.HU_TYPE_GUADAFENG);
				}else{
					huInfoList.add(Cnst.HU_TYPE_MOBAO);
				}
			}
			//将宝牌还原--先检测边宝（3和7）
			if(needCheck){
				if(dongZuoPai==32){
					//肯定是宝中宝
					huInfoList.add(Cnst.HU_TYPE_BAOZHONGBAO);
				}else{//检测是不是边宝和漏宝
					int paiNum=(dongZuoPai)%9;
					if(paiNum==0){
						paiNum=9;
					}
					//边宝
					if(paiNum==3){
						//必须1的位置大于0  或者8的位置大于0
						if(shouPaiArr[dongZuoPai-3]>0 &&  shouPaiArr[dongZuoPai-2]>0){
							int y = shouPaiArr[dongZuoPai-3];
							int j = shouPaiArr[dongZuoPai-2];
							shouPaiArr[dongZuoPai-3]=y-1;
							shouPaiArr[dongZuoPai-2]=j-1;
							if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
								huInfoList.add(Cnst.HU_TYPE_BIANBAO);
								needCheck=false;
							}
							//回复原来的数量
							shouPaiArr[dongZuoPai-3]=y;
							shouPaiArr[dongZuoPai-2]=j;
						}
					}else if( paiNum==7){
						if(shouPaiArr[dongZuoPai]>0 &&  shouPaiArr[dongZuoPai+1]>0){
							int y = shouPaiArr[dongZuoPai+1];
							int j = shouPaiArr[dongZuoPai];
							shouPaiArr[dongZuoPai+1]=y-1;
							shouPaiArr[dongZuoPai]=j-1;
							if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
								huInfoList.add(Cnst.HU_TYPE_BIANBAO);
								needCheck=false;
							}
							//回复原来的数量
							shouPaiArr[dongZuoPai+1]=y;
							shouPaiArr[dongZuoPai]=j;
						}
					}
					//检测漏宝
					if(needCheck){
						if( paiNum>1 && paiNum<9){
							if(shouPaiArr[dongZuoPai]>0 &&  shouPaiArr[dongZuoPai-2]>0){
								int y = shouPaiArr[dongZuoPai-2];
								int j = shouPaiArr[dongZuoPai];
								shouPaiArr[dongZuoPai-2]=y-1;
								shouPaiArr[dongZuoPai]=j-1;
								if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
									huInfoList.add(Cnst.HU_TYPE_LOUBAO);
									needCheck=false;
								}
								//回复原来的数量
								shouPaiArr[dongZuoPai-2]=y;
								shouPaiArr[dongZuoPai]=j;
							}
						}
					}
					if(needCheck){
						huInfoList.add(Cnst.HU_TYPE_BAOZHONGBAO);
					}
				}
			}
		}else{//自摸没宝的话--和别人出的牌一样处理
			Integer checkKaBianDiao=0;
			if(isJia){
				if(isGuaDaFeng){
					Integer yiJiu=0;
					if(checkYiJiu(winPlayer, newList)){
						yiJiu=1;
					}
					checkKaBianDiao= checkBianKaDiaoWithHun(shouPaiArr,room,yiJiu);
				}else{
					checkKaBianDiao = checkKaBianDiao(winPlayer, room);
				}
			}
			if(checkKaBianDiao!=0){
				huInfoList.add(checkKaBianDiao);
				
			}else{
				huInfoList.add(Cnst.HU_TYPE_PINGHU);
			}
		}
		
		return huInfoList;
	}

	/**
	 * 检测带混牌的边卡吊
	 * @param shouPaiArr
	 * @param dongZuoPai
	 * @param room 需要检测的
	 * @return
	 */
	private static Integer checkBianKaDiaoWithHun(int[] shouPaiArr,
			 RoomResp room,Integer yiJiu) {
		int x=shouPaiArr.length;
		//检测吊算夹
		if(room.getDanDiaoSuanJia().equals(1)){
			for (int i = 0; i < x; i++) {
				if(shouPaiArr[i]>0){
					int j = shouPaiArr[i];
					//移除一个和混组成吊
					shouPaiArr[i]=j-1;
					if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
						//回复原来的数量
						shouPaiArr[i]=j;
						return Cnst.HU_TYPE_JIAHU_DIAO;
					}else{
						shouPaiArr[i]=j;
					}
				}
			}
		}
		//检测三七算夹---边胡
		if(room.getSanQiSuanJia()==1){
			//检测三七卡
			//必须1的位置大于0  或者8的位置大于0
			for (int i = 0; i <x; i++) {
				if((i+1)%9==1 || (i+1)%9==8 && i<30){//说明这是1或者8的位置
					if(shouPaiArr[i]>0 &&  shouPaiArr[i+1]>0){
						int y = shouPaiArr[i];
						int z = shouPaiArr[i+1];
						shouPaiArr[i]=y-1;
						shouPaiArr[i+1]=z-1;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
							//回复原来的数量
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
							return Cnst.HU_TYPE_JIAHU_BIAN;
						}else{
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
						}
					}
				}
			}
		}
		//检测卡
		for (int i = 0; i < x; i++) {
			if((i+1)%9<8 ){//说明找到1到7  8和9没有人与其组成卡
				if(shouPaiArr[i]>0 &&  shouPaiArr[i+2]>0){
					int y = shouPaiArr[i];
					int z = shouPaiArr[i+2];
					shouPaiArr[i]=y-1;
					shouPaiArr[i+2]=z-1;
					if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
						//回复原来的数量
						shouPaiArr[i]=y;
						shouPaiArr[i+2]=z;
						return Cnst.HU_TYPE_JIAHU_JIA;
					}else{
						shouPaiArr[i]=y;
						shouPaiArr[i+2]=z;
					}
				}
			}
		}
		//检测无1
		if(yiJiu==0){//没有1,9
			for (int i = 0; i < x; i++) {
				if((i+1)%9==2){//说明是2
					//
					if(shouPaiArr[i]>0 &&  shouPaiArr[i+1]>0){
						int y = shouPaiArr[i];
						int z = shouPaiArr[i+1];
						shouPaiArr[i]=y-1;
						shouPaiArr[i+1]=z-1;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
							//回复原来的数量
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
							return Cnst.HU_TYPE_JIAHU_WUYIHUYI;
						}else{
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
						}
					}
				}else if((i+1)%9==7){//说明是7
					//7和8是否都大于0
					if(shouPaiArr[i]>0 &&  shouPaiArr[i+1]>0){
						int y = shouPaiArr[i];
						int z = shouPaiArr[i+1];
						shouPaiArr[i]=y-1;
						shouPaiArr[i+1]=z-1;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, 34)){
							//回复原来的数量
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
							return Cnst.HU_TYPE_JIAHU_WUYIHUYI;
						}else{
							shouPaiArr[i]=y;
							shouPaiArr[i+1]=z;
						}
					}
				}
			}
		}
		
		return 0;
	}

	public static List<Integer> paiXu(List<Integer> pais) {
		Collections.sort(pais);
		return pais;
	}

	
	
	
	
	
	
	
	
	
	
}
