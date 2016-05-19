/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 18 Apr 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import java.util.List;
import java.util.Vector;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
public class CoverageArea {

    private Profile planProfile;
    private PlanType plan;
    private GraphType planGraph;
    private List<ManeuverLocation> path;

    public CoverageArea(String id, Profile planProfile, MapDecomposition areaToCover, MissionType mt) {
        this.planProfile = planProfile;

        path = getPath(areaToCover);
        plan = getPlan(mt, path, id);
    }


    private List<ManeuverLocation> getPath(MapDecomposition areaToCover) {
        if(areaToCover.getClass().getSimpleName().toString().equals("GridArea"))
            return new SpiralSTC((GridArea) areaToCover).getPath();

        /* TODO implement for other types of decompositions */
        return null;
    }

    /**
     * Generates a PlanType for a coverage area plan
     * */
    private PlanType getPlan(MissionType mt, List<ManeuverLocation> path, String id) {
        /* returns an empty plan type */
        if(path.isEmpty())
            return new PlanType(mt);

        FollowPath fpath = asFollowPathManeuver(path);
        PlanType ptype = new PlanType(mt);
        ptype.getGraph().addManeuver(fpath);
        ptype.setId(id);

        return ptype;
    }

    public ManeuverLocation getManeuverLocation(Profile planProfile, LocationType lt) {
        ManeuverLocation manLoc = new ManeuverLocation(lt);
        manLoc.setZ(planProfile.getProfileZ());

        /* TODO set according to profile's parameters */
        manLoc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        return manLoc;
    }

    /**
     * Generates a GraphType for a coverage area plan
     * */
    public GraphType asGraphType() {
        return planGraph;
    }

    public FollowPath asFollowPathManeuver() {
        return asFollowPathManeuver(this.path);
    }

    private FollowPath asFollowPathManeuver(List<ManeuverLocation> path) {
        FollowPath fpath = new FollowPath();
        ManeuverLocation loc = path.get(0);

        fpath.setManeuverLocation(getManeuverLocation(planProfile, loc));
        fpath.setSpeed(planProfile.getProfileSpeed());

        /* TODO set according to profile's parameters */
        fpath.setSpeedUnits(ManeuverLocation.Z_UNITS.DEPTH.toString());

        Vector<double[]> offsets = new Vector<>();
        for(ManeuverLocation point : path) {
            double[] newPoint = new double[4];
            double[] pOffsets = point.getOffsetFrom(loc);

            newPoint[0] = pOffsets[0];
            newPoint[1] = pOffsets[1];
            newPoint[2] = pOffsets[2];

            offsets.add(newPoint);
        }

        fpath.setOffsets(offsets);

        return fpath;
    }

    public PlanType asPlanType() {
        return plan;
    }

    /**
     * Returns a PlanSpecification of the plan.
     * If the plan is empty returns null
     * */
    public PlanSpecification asPlanSpecification() {
        if(plan.isEmpty())
            return null;

        PlanSpecification planSpec = (PlanSpecification) plan.asIMCPlan();
        planSpec.setValue("description", "Coverage plan automatically generated by MVPlanning");

        return planSpec;
    }
}
