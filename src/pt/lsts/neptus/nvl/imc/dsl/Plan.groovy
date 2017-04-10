package pt.lsts.neptus.nvl.imc.dsl;

import java.util.List;
import pt.lsts.imc.CompassCalibration
import pt.lsts.imc.Loiter
import pt.lsts.imc.Maneuver as IMCManeuver
import pt.lsts.imc.PlanSpecification
import pt.lsts.imc.PlanControl
import pt.lsts.imc.PlanManeuver
import pt.lsts.imc.PlanTransition
import pt.lsts.imc.Rows
import pt.lsts.imc.Goto
import pt.lsts.imc.StationKeeping
import pt.lsts.imc.YoYo
import pt.lsts.imc.Launch
import pt.lsts.imc.PopUp
import pt.lsts.imc.CompassCalibration.DIRECTION
import pt.lsts.imc.net.*
import pt.lsts.neptus.comm.IMCSendMessageUtils
import pt.lsts.neptus.comm.manager.imc.ImcSystem
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder
import pt.lsts.neptus.types.mission.plan.PlanType
import pt.lsts.neptus.types.mission.ConditionType
import pt.lsts.neptus.types.mission.MissionType
import pt.lsts.neptus.types.mission.TransitionType
import pt.lsts.neptus.console.ConsoleLayout
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.mp.Maneuver as NeptusManeuver
import pt.lsts.neptus.mp.maneuvers.Launch as NeptusLaunch
import pt.lsts.neptus.mp.maneuvers.PopUp  as NeptusPopUp
import pt.lsts.neptus.mp.maneuvers.RowsManeuver
import pt.lsts.neptus.mp.maneuvers.StationKeeping as NeptusStationKeeping
import pt.lsts.neptus.mp.maneuvers.Loiter  as NeptusLoiter 
import pt.lsts.neptus.mp.maneuvers.Goto  as NeptusGoto
import pt.lsts.neptus.params.ManeuverPayloadConfig
/**
 * DSL to generate IMC plans (maneuvers)
 * ported to groovy from  https://github.com/zepinto/imc-kotlin 
 * @author keila
 *
 */

class Plan {
	String plan_id
	String description
	Speed speed
	Z z
	Location location
	List<PlanManeuver> mans = new ArrayList <>()
    List<NeptusManeuver> neptus_mans = new ArrayList <>()
	int count
    String [] vehicles_id
    String otherPayloads //Communications means and Acoustics requirements //TODO
	Plan(String id){
		plan_id = id
		count = 1
		location = Location.APDL
		speed = new Speed(1.0, Speed.Units.METERS_PS)
		z = new Z(0.0,Z.Units.DEPTH)
		
	}

	 def IMCManeuver maneuver(String id, Class maneuver) {
        
		def man = maneuver.newInstance(
				lat: location.latitude,
				lon: location.longitude,
				speed: speed.value,
				z:   z.value,
				ZUnitsStr: z.getUnits(),
				speedUnitsStr: speed.getUnits()
				)
		mans.add(planFromMan(id,man))
		man
	}
    
     def PlanManeuver planFromMan(String id,IMCManeuver man) {
         
         def planFromMan = new PlanManeuver (
             maneuverId: id,
             data: man
           )
         planFromMan
     }


	 //default values on args to current fields -> Named Parameters are Converted to Map
	 // see -> http://stackoverflow.com/questions/15393962/groovy-named-parameters-cause-parameter-assignments-to-switch-any-way-around-th

	 def IMCManeuver goTo (LinkedHashMap params){
		 def id = "$count"+".Goto"
         def payload
         count++
		 if(params!=null){
		 params.with{
			 if(speed!= null)
			 	this.speed = speed
			 if(z!= null)
			 	this.z = z
			 if(location!= null)
			 	this.location = location
			 if(params['id'] != null){
			 	id = params.id
			 }
             if(params['payload'] != null){
                 if(params.payload['name'] != null){
                     def payload_name = params.payload['name']
                     payload = new Payload(payload_name)
                     //TODO Another Map to payload parameters???
                     if(params.payload['range'] != null){
                         def payload_range = params.payload['range']
                         payload.range payload_range
                     }
                     if(params.payload['frequency'] != null){
                         def payload_frequency = params.payload['frequency']
                         payload.frequency = payload_frequency
                     }
                     if(params.payload['active'] != null){
                         def payload_active = params.payload['active']
                         payload.active payload_active
                     }
                 }
                 else 
                     println "The name of the payload required must be provided."              }
		 }
	}
	 	def man = maneuver(id,Goto)
         NeptusGoto go  = new pt.lsts.neptus.mp.maneuvers.Goto()
         if (payload != null){
             go.setProperties(payload.properties("Goto"))
             //man.setProperties(payload.properties())//TODO for standalone version!
         }
         go.parseIMCMessage(man)
         go.setId(id)
         neptus_mans.add(go)
        
        man
	} 


	def IMCManeuver loiter(LinkedHashMap params){
        def payload
		double duration =  60.0,radius=20.0
		def id = "${this.count++}"+".Loiter"
		if (params != null){
		 if(params['duration']!=null)
		 	 duration= params.duration
	     if(params['radius']!=null)
			  radius= params.radius
		 if(params['speed']!= null)
		 	this.speed = params.speed
		 if(params['z']!= null)
			 this.z = params.z
		 if(params['location']!= null)
			 this.location = params.location
		 if(params['id'] != null)
			 id = params.id
         if(params['payload'] != null){
             if(params.payload['name'] != null){
                 def payload_name = params.payload['name']
                 payload = new Payload(payload_name)
                 //TODO Another Map to payload parameters???
                 if(params.payload['range'] != null){
                     def payload_range = params.payload['range']
                     payload.range payload_range
                 }
                 if(params.payload['frequency'] != null){
                     def payload_frequency = params.payload['frequency']
                     payload.frequency = payload_frequency
                 }
                 if(params.payload['active'] != null){
                     def payload_active = params.payload['active']
                     payload.active payload_active
                 }
             }
             else
                 println "The name of the payload required must be provided." //TODO
         }
		}
		Loiter man = maneuver(id,Loiter)
        
		man.setDuration = duration.intValue() 
		man.radius   = radius
        man.setType(Loiter.TYPE.CIRCULAR)
        
        
        NeptusLoiter loiter  = new pt.lsts.neptus.mp.maneuvers.Loiter()
        loiter.parseIMCMessage(man)
        loiter.setId(id)
        if (payload != null){
            loiter.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add(loiter)
		man
		
	}

	//def yoyo(double max_depth=20.0,double min_depth=2.0,Speed speed = this.speed, Z z = this.z,Location loc= this.location,String id="${count++}"+".YoYo"){
	def IMCManeuver yoyo(LinkedHashMap params){
        def payload
		double max_depth=20.0,min_depth=2.0
		def id = "${count++}"+".YoYo"
		if (params != null){
		 if(params['max_depth']!=null)
			  max_depth= params.max_depth
		 if(params['min_depth']!=null)
			  min_depth= params.min_depth
		 if(params['speed']!= null)
			 this.speed = params.speed
		 if(params['z']!= null)
			 this.z = params.z
		 if(params['location']!= null)
			 this.location = params.location
		 if(params['id'] != null)
			 id = params.id
         if(params['payload'] != null){
             if(params.payload['name'] != null){
                 def payload_name = params.payload['name']
                 payload = new Payload(payload_name)
                 //TODO Another Map to payload parameters???
                 if(params.payload['range'] != null){
                     def payload_range = params.payload['range']
                     payload.range payload_range
                 }
                 if(params.payload['frequency'] != null){
                     def payload_frequency = params.payload['frequency']
                     payload.frequency = payload_frequency
                 }
                 if(params.payload['active'] != null){
                     def payload_active = params.payload['active']
                     payload.active payload_active
                 }
             }
             else
                 println "The name of the payload required must be provided." //TODO
         }
		}
		
		YoYo man = maneuver(id, YoYo)
		
		man.amplitude = (max_depth - min_depth)
		man.z         = (max_depth + min_depth) / 2.0
        
        pt.lsts.neptus.mp.maneuvers.YoYo yoyo = new pt.lsts.neptus.mp.maneuvers.YoYo()
        yoyo.parseIMCMessage(man)
        yoyo.setId(id)
        if (payload != null){
            yoyo.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add yoyo
		man
		
	}


	//def popup(double duration=180.0,boolean currPos=true,Speed speed = this.speed, Z z = this.z,Location loc= this.location,String id="{count++}"+".Popup"){
	def IMCManeuver popup(LinkedHashMap params) {
        def payload
		double duration = 0.0
		def currPos = true
		def id = "${this.count++}"+".PopUp"
		if (params != null){
		 if(params['duration']!=null)
			  duration= params.duration
		 if(params['currPos']!=null)
			  currPos= params.currPos
		 if(params['speed']!= null)
			 this.speed = params.speed
		 if(params['z']!= null)
			 this.z = params.z
		 if(params['location']!= null)
			 this.location = params.location
		 if(params['id'] != null)
			 id = params.id
         if(params['payload'] != null){
             if(params.payload['name'] != null){
                 def payload_name = params.payload['name']
                 payload = new Payload(payload_name)
                 //TODO Another Map to payload parameters???
                 if(params.payload['range'] != null){
                     def payload_range = params.payload['range']
                     payload.range payload_range
                 }
                 if(params.payload['frequency'] != null){
                     def payload_frequency = params.payload['frequency']
                     payload.frequency = payload_frequency
                 }
                 if(params.payload['active'] != null){
                     def payload_active = params.payload['active']
                     payload.active payload_active
                 }
             }
             else
                 println "The name of the payload required must be provided." //TODO
         }
		}
		 
		
		def man = maneuver(id, PopUp)
		
		man.duration = duration.intValue()
		man.flags    = currPos ? PopUp.FLG_CURR_POS : 0
        
        NeptusPopUp popup = new pt.lsts.neptus.mp.maneuvers.PopUp()
        popup.parseIMCMessage(man)
        popup.setId(id)
        if (payload != null){
            popup.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add popup
		man
		
	}
	
	//def skeeping(double radius=20.0,double duration=0 ,Speed speed = this.speed, Z z = this.z,Location loc= this.location,String id="{count++}"+".StationKeeping") {
	def IMCManeuver skeeping(LinkedHashMap params){
        def payload
		double duration = 60.0,radius=20.0
		def id = "${this.count++}"+".StationKeeping"
		if (params != null){
		 if(params['duration']!=null)
			  duration= params.duration
		 if(params['radius']!=null)
			  radius= params.radius
		 if(params['speed']!= null)
			 this.speed = params.speed
		 if(params['z']!= null)
			 this.z = params.z
		 if(params['location']!= null)
			 this.location = params.location
		 if(params['id'] != null)
			 id = params.id
         if(params['payload'] != null){
             if(params.payload['name'] != null){
                 def payload_name = params.payload['name']
                 payload = new Payload(payload_name)
                 //TODO Another Map to payload parameters???
                 if(params.payload['range'] != null){
                     def payload_range = params.payload['range']
                     payload.range payload_range
                 }
                 if(params.payload['frequency'] != null){
                     def payload_frequency = params.payload['frequency']
                     payload.frequency = payload_frequency
                 }
                 if(params.payload['active'] != null){
                     def payload_active = params.payload['active']
                     payload.active payload_active
                 }
             }
             else
                 println "The name of the payload required must be provided." //TODO
         }
		}
		 
		StationKeeping man = maneuver(id, StationKeeping)
		man.duration = duration.intValue()
		man.radius   = radius
        
        NeptusStationKeeping skeeping = new pt.lsts.neptus.mp.maneuvers.StationKeeping()
        skeeping.parseIMCMessage(man)
        skeeping.setId(id)
        if (payload != null){
            skeeping.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add skeeping
        
		man
		}
	
	def IMCManeuver compassCalibration(LinkedHashMap params){
		
		double amplitude=1,duration=300,radius=5,pitch=15
		DIRECTION direction = DIRECTION.CLOCKW
        def payload
		def id = "${count++}"+".CompassCalibration"
		if (params != null){
		 if(params['duration']!=null)
			  duration= params.duration
		 if(params['radius']!=null)
			  radius= params.radius
		if(params['amplitude']!=null)
			amplitude= params.amplitude
		if(params['speed']!= null)
			 this.speed = params.speed
		 if(params['z']!= null)
			 this.z = params.z
		 if(params['location']!= null)
			 this.location = params.location
		 if(params['id'] != null)
			 id = params.id
         if(params['payload'] != null){
             if(params.payload['name'] != null){
                 def payload_name = params.payload['name']
                 payload = new Payload(payload_name)
                 //TODO Another Map to payload parameters???
                 if(params.payload['range'] != null){
                     def payload_range = params.payload['range']
                     payload.range payload_range
                 }
                 if(params.payload['frequency'] != null){
                     def payload_frequency = params.payload['frequency']
                     payload.frequency = payload_frequency
                 }
                 if(params.payload['active'] != null){
                     def payload_active = params.payload['active']
                     payload.active payload_active
                 }
             }
             else
                 println "The name of the payload required must be provided." //TODO
         }
		}
		 
		CompassCalibration man = maneuver(id, CompassCalibration)
		
		man.duration  = duration.intValue()
		man.direction = direction
		man.radius    = radius
		man.amplitude = amplitude
        
        pt.lsts.neptus.mp.maneuvers.CompassCalibration cc = new pt.lsts.neptus.mp.maneuvers.CompassCalibration()
        cc.parseIMCMessage(man)
        cc.setId(id)
        if (payload != null){
            cc.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add cc
        
		man
		}
	
	

	def IMCManeuver launch(LinkedHashMap params)  {
		String id="${count++}"+".Launch"
        def payload
		if (params != null){
			if(params['speed']!= null)
				this.speed = params.speed
			if(params['z']!= null)
				this.z = params.z
			if(params['location']!= null)
				this.location = params.location
			if(params['id'] != null)
				id = params.id
            if(params['payload'] != null){
                if(params.payload['name'] != null){
                    def payload_name = params.payload['name']
                    payload = new Payload(payload_name)
                    //TODO Another Map to payload parameters???
                    if(params.payload['range'] != null){
                        def payload_range = params.payload['range']
                        payload.range payload_range
                    }
                    if(params.payload['frequency'] != null){
                        def payload_frequency = params.payload['frequency']
                        payload.frequency = payload_frequency
                    }
                    if(params.payload['active'] != null){
                        def payload_active = params.payload['active']
                        payload.active payload_active
                    }
                }
                else
                    println "The name of the payload required must be provided." //TODO
            }
		   }
        
        def man = maneuver(id,Launch)
        
        NeptusLaunch launch = new pt.lsts.neptus.mp.maneuvers.Launch()
        launch.parseIMCMessage(man)
        launch.setId(id)
        if (payload != null){
            launch.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add launch
        man
        
	}
	
	def IMCManeuver rows(LinkedHashMap params){
		double bearing=0.0,cross_angle=0.0,width=100,length=200,hstep=27.0
		short coff=15,flags
        def payload
		String id="${count++}"+".Rows"
		if (params != null){
			if(params['bearing']!= null)
				bearing = params.bearing
			if(params['width']!= null)
				width = params.width
			if(params['cross_angle']!= null)
				cross_angle = params.cross_angle				
			if(params['length']!= null)
				length = params.length
			if(params['hstep']!= null)
				hstep = params.hstep
			if(params['cross_angle']!= null)
				cross_angle = params.cross_angle
			if(params['curvOff']!= null)
				coff = params.curvOff
			if(params['coff']!= null)
				coff = params.coff
			if(params['flags']!= null)
				flags = params.flags
			if(params['id'] != null)
				id = params.id
			if(params['speed']!= null)
				this.speed = params.speed
			if(params['z']!= null)
				this.z = params.z
			if(params['location']!= null)
				this.location = params.location
			if(params['id'] != null)
				id = params.id
            if(params['payload'] != null){
                if(params.payload['name'] != null){
                    def payload_name = params.payload['name']
                    payload = new Payload(payload_name)
                    //TODO Another Map to payload parameters???
                    if(params.payload['range'] != null){
                        def payload_range = params.payload['range']
                        payload.range payload_range
                    }
                    if(params.payload['frequency'] != null){
                        def payload_frequency = params.payload['frequency']
                        payload.frequency = payload_frequency
                    }
                    if(params.payload['active'] != null){
                        def payload_active = params.payload['active']
                        payload.active payload_active
                    }
                }
                else
                    println "The name of the payload required must be provided." //TODO
            }
		   }
		Rows man = maneuver(id,Rows)
		man.bearing        = bearing
		man.crossAngle     = cross_angle
		man.width          = width
		man.length         = length
		man.hstep          = hstep
		man.coff           = coff
		if(flags!=null){
            //TODO rows.setFlags(flags)
            man.setFlags flags
            //rows.setFlags((short) ((squareCurve ?  : 0) + (firstCurveRight ? Rows.FLG_CURVE_RIGHT : 0)));
            //Rows.FLG_SQUARE_CURVE ? rows.squareCurve : 1
            //Rows.FLG_CURVE_RIGHT  ? rows.firstCurveRight : 1
            }
        	
            
            
        RowsManeuver rows  = new RowsManeuver()
        rows.parseIMCMessage(man)
        rows.setId(id)
        if (payload != null){
            rows.setProperties(payload.properties("Goto"))
            //man.setProperties(payload.properties())//TODO for standalone version!
        }
        neptus_mans.add(rows)
		man

	}
	
	def locate(double latitude, double longitude) {
		def loc = new Location(latitude, longitude)
		this.location = loc
	}
	def locate(Angle latitude, Angle longitude) {
		def loc = new Location(latitude, longitude)
		this.location = loc
	}
	def locate(Location loc=this.location) {
		this.location = loc
		
	}
	
	def move(double northing, double easting){
		this.location= location.translateBy(northing,easting)
	}
    
    /**
     * Vehicles to send this plan
     */
    def void setVehicles(String...vehicles) {
        vehicles_id = vehicles
    }
    
    /**
     * 
     * @param plan
     * @return null if is in sync w/ all vehicles otherwise returns a list of the vehicles not in  sync 
     */
    def String[] syncPlanVehicles(PlanType plan){
        String notInSync =""
        ImcSystem sys
        //TODO
        if(vehicles_id == null){
            println "Please define the vehicles to send this plan"
            return
        }
        vehicles_id.each {
            if((sys = ImcSystemsHolder.lookupSystemByName(it)) != null) {
                PlanDBState prs = sys.getPlanDBControl().getRemoteState();
                if (prs == null || !prs.matchesRemotePlan(plan)) {
                    notInSync += (notInSync.length() > 0 ? ", " : "") + it
                    
                }
            }
            
        }
        
        {notInSync}
    }
    

	private def List<PlanTransition> maneuver_transitions(){
		
		def trans = new ArrayList<PlanTransition>()
		PlanManeuver previous = null
		mans.each {
			if (previous != null) {
			
			def transition = new PlanTransition(
				sourceMan: previous.maneuverId,
				destMan: it.maneuverId,
				conditions: "maneuverIsDone"
			)
		trans += transition
	   }
		previous = it
	}
		trans
} 
		
		
		
	 

	def PlanSpecification asPlanSpecification() {
		PlanSpecification ps = new PlanSpecification()
		ps.description = description
		ps.planId = plan_id
		ps.startManId = mans[0].getManeuverId()
		ps.setManeuvers mans
		ps.setTransitions maneuver_transitions()
		ps
	}
    
    def PlanType asPlanType(ConsoleLayout console) {
        
        def plantype = new PlanType(console.getMission())
        def transitions = maneuver_transitions()
        
        //Add Maneuvers
        neptus_mans.each{
            plantype.getGraph().addManeuver(it)
        }
    
        //TODO possibly add payloads or other requirements
        
        //Add Transitions
        plantype.getGraph().setInitialManeuver(neptus_mans[0].getId())
        transitions.each{
            
            if(it.getSourceMan()!=null) 
              plantype.getGraph().addTransition(it.getSourceMan(),it.getDestMan(),it.getConditions())
                
            }
            
        plantype.setId(plan_id)
        if(vehicles_id==null)
            vehicles_id = {"lauv-xplore-1"}
        vehicles_id.each { plantype.setVehicle(it) }
        plantype.setMissionType(console.getMission())
        console.getMission().getIndividualPlansList().put(plan_id,plantype)
        console.getMission().save(true)
        console.updateMissionListeners()
        plantype
     }

	void sendTo (String vehicle,long milis=60000) {
		def plan_spec = this.asPlanSpecification()
		def select = false
		PlanControl plan = 	new PlanControl(
			opStr: 'LOAD',
			planId: this.plan_id,
			arg: plan_spec
			)
        
		ImcSystemsHolder.lookupActiveSystemVehicles().each { //Through Groovy Plugin  
            
			if(it.getId().equals(vehicle))
			select = true
		}
        def error_msg = "Error sending plan: "+plan_id+" to "+vehicle
		//while(!select.equals(null)) vehicles_id.each {protocol.connect(it); def select = protocol.waitfor(it,milis)}
		if (select != null && IMCSendMessageUtils.sendMessage(plan,error_msg,true, vehicle)) //IMCSendMessageUtils.sendMessage(plan,false,vehicle)
			println ("$plan_id commanded to $vehicle")
		else
			println ("Error communicating with $vehicle")
	}
	
	
	void addToConsole(ConsoleLayout console) {
        //console <- binding variable

		def plan   = this.asPlanSpecification()
        plan.planId = this.plan_id
        plan.description = this.description
        
		def neptus_plan = this.asPlanType(console)
		console.getMission().addPlan(neptus_plan) 
	}

}
