/*
 * Copyright (C) 2016 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.format.structure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.constants.format.KmlConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.exception.ODKParseException;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionField;
import org.opendatakit.aggregate.submission.SubmissionSet;
import org.opendatakit.aggregate.submission.SubmissionValue;
import org.opendatakit.aggregate.submission.type.GeoPoint;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

/**
 *
 * @author wbrunette@gmail.com
 *
 */
public class KmlGeoTraceNGeoShapeGenerator extends AbstractKmlElementBase {
  private static final String LIMITATION_MSG = "limitation: name must be in the submission (top-level) or must be in the same repeat group as the trace/shape";

  private static final String PARSE_PROBLEM_GEOTRACE_OR_GEOSHAPES_STRING = "Problem with GPS Coordinates parsed from geotrace or geoshapes. This is not properly formatted coordinate:";

  private FormElementModel nameElement;

  public KmlGeoTraceNGeoShapeGenerator(FormElementModel geoField, FormElementModel nameField) {
    super(geoField);

    // Verify that nesting constraints hold.
    if (verifyElementSameLevel(nameField)) {
      nameElement = nameField;
    } else {
      throw new IllegalStateException(LIMITATION_MSG);
    }

  }

  boolean childVerifyFieldsArePresent(List<FormElementModel> elements) {
    if (nameElement != null && !elements.contains(nameElement)) {
      return false;
    }
    return true;
  }

  @Override
  String generatePlacemarkSubmission(Submission sub, List<FormElementModel> propertyNames,
      CallingContext cc) throws ODKDatastoreException {

    try {
      // parse into geopoints
      List<GeoPoint> points = getGeoLineCoordinates(sub.getElementValue(getGeoElement()));
      if (!points.isEmpty()) {  
        return generateFormattedGeoTrace(points, getName(sub));
      }
    } catch (ODKParseException e) {
      // TODO: CHANGE SO THE ERROR GOES TO THE UI
      e.printStackTrace();
    }
    return BasicConsts.EMPTY_STRING;
  }

  private String getName(SubmissionSet set) {
    SubmissionValue nameField = set.getElementValue(nameElement);
    if (nameField != null) {
      Object nameValue = ((SubmissionField<?>) nameField).getValue();
      if (nameValue != null) {
        return nameValue.toString();
      }
    }
    return null;
  }
  
  private String generateFormattedGeoTrace(List<GeoPoint> points, String name) {
    StringBuilder coordinateString = new StringBuilder(BasicConsts.EMPTY_STRING);
    for (GeoPoint gp : points) {
      if (gp != null) {
        if (gp.getLatitude() != null && gp.getLongitude() != null) {
          coordinateString.append(gp.getLongitude());
          coordinateString.append(BasicConsts.COMMA);
          coordinateString.append(gp.getLatitude());
          if (gp.getAltitude() != null) {
            coordinateString.append(BasicConsts.COMMA);
            coordinateString.append(gp.getAltitude());
          }
        }
        coordinateString.append(BasicConsts.NEW_LINE);
      }
    }
    
    String nameStr = (name == null) ? BasicConsts.EMPTY_STRING : name;
    return String.format(KmlConsts.KML_LINE_STRING_PLACEMARK_TEMPLATE, nameStr, coordinateString
        .toString().trim());
  }

  private List<GeoPoint> getGeoLineCoordinates(SubmissionValue subValue) throws ODKParseException {
    if (subValue != null) {
      Object value = ((SubmissionField<?>) subValue).getValue();
      if (value != null && value instanceof String) {
        return KmlGeoTraceNGeoShapeGenerator.parseGeoLineCoordinates((String) value);
      }
    }
    return new ArrayList<GeoPoint>(); // return empty array list
  }

  static List<GeoPoint> parseGeoLineCoordinates(String stringWithCoordinates)
      throws ODKParseException {
    List<GeoPoint> points = new ArrayList<GeoPoint>();
    if (stringWithCoordinates != null) {
      String[] geoPointStrings = stringWithCoordinates.split(BasicConsts.SEMI_COLON);
      for (String gpsCoordinate : geoPointStrings) {
        try {

          String[] values = gpsCoordinate.trim().split("\\s+");
          GeoPoint coordinates;
          if (values.length == 1) {
            if (BasicConsts.EMPTY_STRING.equals(values[0])) {
              continue;
            } else {
              throw new ODKParseException(PARSE_PROBLEM_GEOTRACE_OR_GEOSHAPES_STRING + gpsCoordinate);
            }
          } else if (values.length == 2) {
            coordinates = new GeoPoint(new BigDecimal(values[0]), new BigDecimal(values[1]));
          } else if (values.length == 3) {
            coordinates = new GeoPoint(new BigDecimal(values[0]), new BigDecimal(values[1]),
                new BigDecimal(values[2]));
          } else if (values.length == 4) {
            coordinates = new GeoPoint(new BigDecimal(values[0]), new BigDecimal(values[1]),
                new BigDecimal(values[2]), new BigDecimal(values[3]));
          } else {
            throw new ODKParseException(PARSE_PROBLEM_GEOTRACE_OR_GEOSHAPES_STRING);
          }
          points.add(coordinates);
        } catch (Throwable e) {
          throw new ODKParseException(PARSE_PROBLEM_GEOTRACE_OR_GEOSHAPES_STRING + gpsCoordinate, e);
        }
      }
    }
    return points;
  }

}
