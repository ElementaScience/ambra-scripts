/* $HeadURL::                                                                                    $
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
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
 */

package org.topazproject.ambra.sip

import org.topazproject.ambra.util.ToolHelper

/*
 * Prepare a SIP from an AP zip. This goes through the following steps:
 * <ol>
 *   <li>create and add a manifest to the zip
 *   <li>fix up the article links
 *   <li>perform validation checks on the SIP and article
 * </ol>
 *
 * @author Ronald Tschalär
 */

args = ToolHelper.fixArgs(args)
String usage = 'PrepareSIP [-vf] [-c <config-overrides.xml>] [-o <output.zip>] <article.zip>'
def cli = new CliBuilder(usage: usage, writer : new PrintWriter(System.out))
cli.h(longOpt:'help', "help (this message)")
cli.o(args:1, 'output.zip - new zip file containing prepared sip; if not specified\n' +
              '             then input file is overwritten')
cli.c(args:1, 'config-overrides.xml - overrides /etc/topaz/ambra.xml')
cli.v(args:0, 'verbose')
cli.f(args:0, 'force - force a new manifest to be created even if one already exists')

def opt = cli.parse(args);

String[] otherArgs = opt.arguments()
if (opt.h || otherArgs.size() != 1) {
  cli.usage()
  return
}

println("Loading configuration...")
def config = ToolHelper.loadConfiguration(opt.c)
def inp = otherArgs[0]
def out = opt.o ?: inp
println("Processing input file: " + inp)

try {
  boolean hasManif = new ArchiveFile(inp).entries().iterator()*.name.contains(SipUtil.MANIFEST)
  if (opt.f || !hasManif) {
    new AddManifest().addManifest(inp, out)
//    println "  manifest ${hasManif ? 'replaced' : 'added'}"
    inp = out
  } else {
//    println "  manifest already present"
  }

    // Update the links in the article

    println("Starting to fix up links in the article... ")
    new FixArticle().fixLinks(inp, out)
    println "Article links fixed successfully.\n"

    // Process the images to create the correctly sized versions for display

    println("Starting to process images... ")
    new ProcessImages(config, opt.v).processImages(out, null)
    println "Images processed successfully.\n"

    // Validate

    println("Starting to validate the article... ")
    new ValidateSIP().validate(out)
    println "Validation complete: No problems found.\n"

    println "Processing complete."
    System.exit(0)
} catch (Exception e) {
    println("Error detected during processing: " + e.getMessage() + "\n")
    e.printStackTrace()

  System.exit(1)
}
