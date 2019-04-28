package core.parsers

import deltas.json.JsonLanguage
import org.scalatest.FunSuite
import util.SourceUtils

class PerformanceTest extends FunSuite {

  test("test whether correct inputs always return a ready in one go") {
    val input = """{
                  |  "AWSTemplateFormatVersion" : "2010-09-09",
                  |
                  |  "Description" : "AWS CloudFormation Sample Template AutoScalingMultiAZWithNotifications: Create a multi-az, load balanced and Auto Scaled sample web site running on an Apache Web Serever. The application is configured to span all Availability Zones in the region and is Auto-Scaled based on the CPU utilization of the web servers. Notifications will be sent to the operator email address on scaling events. The instances are load balanced with a simple health check against the default web page. **WARNING** This template creates one or more Amazon EC2 instances and an Application Load Balancer. You will be billed for the AWS resources used if you create a stack from this template.",
                  |
                  |  "Parameters" : {
                  |
                  |    "VpcId" : {
                  |      "Type" : "AWS::EC2::VPC::Id",
                  |      "Description" : "VpcId of your existing Virtual Private Cloud (VPC)",
                  |      "ConstraintDescription" : "must be the VPC Id of an existing Virtual Private Cloud."
                  |    },
                  |  }
                  |}""".stripMargin
    val result = JsonLanguage.language.compileString(input)
  }

  test("correct JSON performance") {
    val source = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val json = JsonLanguage.language
    val multiplier = 1
    val tenTimesSource = s"[${1.to(10).map(_ => source).reduce((a,b) => a + "," + b)}]"

    val timeA = System.currentTimeMillis()
    for(_ <- 1.to(multiplier * 10)) {
      json.compileString(source)
    }

    val timeB = System.currentTimeMillis()
    for(_ <- 1.to(multiplier)) {
      json.compileString(tenTimesSource)
    }

    val timeC = System.currentTimeMillis()

    val singleSource = timeB - timeA
    val sourceTimesTen = timeC - timeB
    assert(singleSource < 3000 * multiplier)
    System.out.println(s"singleSource:$singleSource")
    System.out.println(s"totalTime:${singleSource + sourceTimesTen}")
  }
}
